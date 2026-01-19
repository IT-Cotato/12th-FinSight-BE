package com.finsight.finsight.domain.naver.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.finsight.domain.ai.domain.service.AiJobService;
import com.finsight.finsight.domain.naver.application.dto.response.NaverCrawlResultResponse;
import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;
import com.finsight.finsight.domain.naver.exception.code.NaverCrawlErrorCode;
import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import com.finsight.finsight.domain.naver.persistence.repository.NaverArticleRepository;
import com.finsight.finsight.global.config.NaverCrawlerProperties;
import com.finsight.finsight.domain.naver.exception.NaverCrawlException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class NaverCrawlerService {

    private final NaverArticleRepository repository;
    private final NaverCrawlerProperties props;

    private final MeterRegistry meterRegistry;

    private final AiJobService aiJobService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final ObjectMapper OM = new ObjectMapper();
    private static final Pattern ARTICLE_PATH = Pattern.compile("/article/(\\d+)/(\\d+)");

    private static final DateTimeFormatter OFFSET_NO_COLON =
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd'T'HH:mm:ssZ")
                    .toFormatter();

    private static final DateTimeFormatter SPACE_DATETIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 전체(8개 섹션) 1회 크롤링 실행
     * - 부분 실패는 계속 진행
     * - "전체가 거의 실패" 케이스는 AppException으로 올려서 API에서 에러코드로 반환
     */
    @Transactional
    public NaverCrawlResultResponse crawlAllOnce() {
        Timer.Sample allSample = Timer.start(meterRegistry);
        CrawlAggregate agg = new CrawlAggregate();

        log.info("[NAVER-CRAWL] start all sections maxPages={}, stopAfterSeenStreak={}, timeoutMs={}",
                props.getMaxPages(), props.getStopAfterSeenStreak(), props.getTimeoutMs());

        List<NaverCrawlResultResponse.CategoryResult> categoryResults = new ArrayList<>();

        for (NaverEconomySection section : NaverEconomySection.values()) {
            CrawlSectionResult r = crawlSection(section);
            agg.add(r);

            categoryResults.add(
                    NaverCrawlResultResponse.CategoryResult.of(
                            section.getDisplayName(),
                            r.scanned,
                            r.saved
                    )
            );
        }

        int sections = NaverEconomySection.values().length;

        // 전 섹션 리스트 페이지 접근 실패 = 차단/네트워크/UA 문제 가능성 높음
        if (agg.allListFailedSections == sections) {
            incAll("all", "list_fail_all_sections");
            throw new NaverCrawlException(NaverCrawlErrorCode.NAVER_LIST_FETCH_FAIL);
        }

        // 저장 0인데 실패만 누적됨 → 외부 실패로 판단
        if (agg.totalSaved == 0 && (agg.listFail + agg.articleFail + agg.parseFail) > 0) {
            incAll("all", "saved_zero_with_fail");
            throw new NaverCrawlException(NaverCrawlErrorCode.NAVER_ARTICLE_FETCH_FAIL);
        }

        allSample.stop(Timer.builder("crawler_run_seconds")
                .tag("scope", "all")
                .register(meterRegistry));

        log.info("[NAVER-CRAWL] end all sections totalScanned={} totalSaved={} listFail={} articleFail={} parseFail={} allListFailedSections={}/{}",
                agg.totalScanned, agg.totalSaved, agg.listFail, agg.articleFail, agg.parseFail, agg.allListFailedSections, sections);

        return NaverCrawlResultResponse.of(agg.totalScanned, agg.totalSaved, categoryResults);
    }

    /**
     * 섹션(탭) 단위 크롤링
     * - stopAfterSeenStreak 로 "중복 연속"이면 조기 종료
     * - 실패는 카운트만 올리고 계속 진행(부분 실패 허용)
     */
    @Transactional
    public CrawlSectionResult crawlSection(NaverEconomySection section) {
        String sectionName = section.getDisplayName();
        CrawlSectionResult result = new CrawlSectionResult(sectionName);

        Timer.Sample sectionSample = Timer.start(meterRegistry);

        int seenStreak = 0;

        log.info("[NAVER-CRAWL] section start: {}", sectionName);

        for (int page = 1; page <= props.getMaxPages(); page++) {
            String listUrl = buildListUrl(section.getUrl(), page);
            result.listPagesTried++;

            Document listDoc;
            try {
                Timer.Sample fetchSample = Timer.start(meterRegistry);
                listDoc = fetch(listUrl);
                fetchSample.stop(Timer.builder("crawler_fetch_seconds")
                        .tag("kind", "list")
                        .tag("section", sectionName)
                        .register(meterRegistry));
                result.listPagesSuccess++;
            } catch (Exception e) {
                result.listFail++;
                log.warn("[NAVER-CRAWL] list fetch fail section={} page={} url={} err={}",
                        sectionName, page, listUrl, e.toString());
                continue;
            }

            List<ArticleId> ids = extractArticleIds(listDoc);
            if (ids.isEmpty()) continue;

            boolean stop = false;

            for (ArticleId id : ids) {
                result.scanned++;
                inc(sectionName, "scanned");

                // DB 중복 체크: (oid, aid)
                boolean exists = repository.existsByOidAndAid(id.oid, id.aid);
                if (exists) {
                    seenStreak++;
                    inc(sectionName, "duplicate_seen");

                    log.debug("[NAVER-CRAWL] already exists section={} oid={} aid={} seenStreak={}",
                            sectionName, id.oid, id.aid, seenStreak);

                    if (seenStreak >= props.getStopAfterSeenStreak()) {
                        inc(sectionName, "stop_after_seen_streak");
                        log.info("[NAVER-CRAWL] stop early section={} reason=seenStreak({}) reached",
                                sectionName, seenStreak);
                        stop = true;
                        break;
                    }
                    continue;
                }

                // 새 기사면 streak reset
                seenStreak = 0;

                String articleUrl = id.canonicalUrl();

                try {
                    Timer.Sample fetchSample = Timer.start(meterRegistry);
                    Document articleDoc = fetch(articleUrl);
                    fetchSample.stop(Timer.builder("crawler_fetch_seconds")
                            .tag("kind", "article")
                            .tag("section", sectionName)
                            .register(meterRegistry));

                    ParsedArticle parsed = parseArticle(articleDoc);

                    // 품질 메트릭
                    if (parsed.publishedAt == null) inc(sectionName, "published_at_null");
                    if (parsed.thumbnailUrl == null) inc(sectionName, "thumbnail_null");

                    // 디버깅: publishedAt NULL 원인 추적 로그
                    if (parsed.publishedAt == null) {
                        log.debug("[NAVER-CRAWL] publishedAt NULL section={} url={} meta={} dataDateTime={} timeDatetime={}",
                                sectionName,
                                articleUrl,
                                safe(attr(articleDoc, "meta[property=article:published_time]", "content")),
                                safe(attr(articleDoc, "span.media_end_head_info_datestamp_time", "data-date-time")),
                                safe(attr(articleDoc, "time[datetime]", "datetime"))
                        );
                    }

                    if (parsed.content == null || parsed.content.isBlank()) {
                        result.parseFail++;
                        inc(sectionName, "parse_fail");

                        log.debug("[NAVER-CRAWL] parse fail(blank content) section={} url={}",
                                sectionName, articleUrl);
                        continue;
                    }

                    NaverArticleEntity entity = NaverArticleEntity.builder()
                            .section(section)
                            .oid(id.oid)
                            .aid(id.aid)
                            .url(articleUrl)
                            .title(parsed.title)
                            .press(parsed.press)
                            .publishedAt(parsed.publishedAt)
                            .thumbnailUrl(parsed.thumbnailUrl)
                            .content(parsed.content)
                            .collectedAt(LocalDateTime.now(KST))
                            .build();

                    try {
                        repository.save(entity);
                        result.saved++;
                        inc(sectionName, "saved");

                        log.info("[NAVER-CRAWL] saved section={} oid={} aid={} publishedAt={} title={}",
                                sectionName, id.oid, id.aid, parsed.publishedAt, truncate(parsed.title, 80));

                        // 본문 길이가 설정된 최소값 미만이면 AI 작업 건너뛰기
                        int minLen = props.getMinContentLengthForAi();
                        int contentLen = parsed.content.length();
                        if (minLen > 0 && contentLen < minLen) {
                            inc(sectionName, "ai_skipped_short_content");
                            log.info("[NAVER-CRAWL] AI job skipped (short content) section={} oid={} aid={} contentLen={} minLen={}",
                                    sectionName, id.oid, id.aid, contentLen, minLen);
                        } else {
                            aiJobService.enqueueSummary(entity, "v1", "gpt-4o-mini");
                        }
                    } catch (DataIntegrityViolationException dup) {
                        // 유니크(oid,aid) 레이스 방지
                        inc(sectionName, "duplicate_race");
                        log.debug("[NAVER-CRAWL] duplicate prevented by DB section={} oid={} aid={}",
                                sectionName, id.oid, id.aid);
                    }

                } catch (Exception e) {
                    result.articleFail++;
                    inc(sectionName, "article_fail");

                    log.warn("[NAVER-CRAWL] article fetch/parse fail section={} url={} err={}",
                            sectionName, articleUrl, e.toString());
                } finally {
                    politeSleep();
                }
            }

            if (stop) break;
        }

        sectionSample.stop(Timer.builder("crawler_run_seconds")
                .tag("scope", "section")
                .tag("section", sectionName)
                .register(meterRegistry));

        log.info("[NAVER-CRAWL] section end: {} scanned={} saved={} listFail={} articleFail={} parseFail={} listPagesTried={} listPagesSuccess={}",
                sectionName, result.scanned, result.saved, result.listFail, result.articleFail, result.parseFail, result.listPagesTried, result.listPagesSuccess);

        return result;
    }

    // =========================
    // Micrometer helpers
    // =========================

    private void inc(String section, String status) {
        Counter.builder("crawler_articles_total")
                .tag("section", section)
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    private void incAll(String scope, String status) {
        Counter.builder("crawler_events_total")
                .tag("scope", scope)
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    private String buildListUrl(String baseUrl, int page) {
        String date = LocalDate.now(KST).format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        return baseUrl + "?date=" + date + "&page=" + page;
    }

    private Document fetch(String url) throws Exception {
        return Jsoup.connect(url)
                .userAgent(props.getUserAgent())
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                .timeout(props.getTimeoutMs())
                .followRedirects(true)
                .get();
    }

    private List<ArticleId> extractArticleIds(Document doc) {
        LinkedHashSet<String> canonUrls = new LinkedHashSet<>();

        for (Element a : doc.select("a[href]")) {
            String href = a.absUrl("href");
            if (href == null || href.isBlank()) continue;

            if (href.contains("/article/") || href.contains("read.naver") || href.contains("n.news.naver.com")) {
                ArticleId id = ArticleId.from(href);
                if (id != null) canonUrls.add(id.canonicalUrl());
            }
        }

        List<ArticleId> out = new ArrayList<>();
        for (String u : canonUrls) {
            ArticleId id = ArticleId.from(u);
            if (id != null) out.add(id);
        }
        return out;
    }

    private ParsedArticle parseArticle(Document doc) {
        String title = firstText(doc, "#title_area", "h2#title_area", "h2.media_end_head_headline");
        String content = firstText(doc, "#dic_area", "#articleBodyContents");
        String press = firstAttr(doc, "alt", "a.media_end_head_top_logo img", ".media_end_head_top_logo img");

        LocalDateTime publishedAt = parsePublishedAt(doc);
        String thumbnailUrl = parseThumbnailUrl(doc);

        return new ParsedArticle(title, press, publishedAt, content, thumbnailUrl);
    }

    private String parseThumbnailUrl(Document doc) {
        String og = attr(doc, "meta[property=og:image]", "content");
        if (og != null && !og.isBlank()) return og.trim();

        String tw = attr(doc, "meta[name=twitter:image]", "content");
        if (tw != null && !tw.isBlank()) return tw.trim();

        Element ld = doc.selectFirst("script[type=application/ld+json]");
        if (ld != null) {
            try {
                JsonNode root = OM.readTree(ld.data());
                JsonNode image = root.get("image");
                if (image != null) {
                    if (image.isTextual()) return image.asText().trim();
                    JsonNode url = image.get("url");
                    if (url != null && url.isTextual()) return url.asText().trim();
                }
            } catch (Exception ignore) {}
        }
        return null;
    }

    private LocalDateTime parsePublishedAt(Document doc) {
        String meta = attr(doc, "meta[property=article:published_time]", "content");
        LocalDateTime t = parseAny(meta);
        if (t != null) return t;

        String dataTime = attr(doc, "span.media_end_head_info_datestamp_time", "data-date-time");
        t = parseAny(dataTime);
        if (t != null) return t;

        String datetime = attr(doc, "time[datetime]", "datetime");
        t = parseAny(datetime);
        if (t != null) return t;

        Element ld = doc.selectFirst("script[type=application/ld+json]");
        if (ld != null) {
            try {
                JsonNode root = OM.readTree(ld.data());
                JsonNode dp = root.get("datePublished");
                if (dp != null && !dp.isNull()) {
                    t = parseAny(dp.asText());
                    if (t != null) return t;
                }
            } catch (Exception ignore) {}
        }

        return null;
    }

    private LocalDateTime parseAny(String raw) {
        if (raw == null || raw.isBlank()) return null;

        try {
            return OffsetDateTime.parse(raw).atZoneSameInstant(KST).toLocalDateTime();
        } catch (DateTimeParseException ignore) {}

        try {
            return OffsetDateTime.parse(raw, OFFSET_NO_COLON).atZoneSameInstant(KST).toLocalDateTime();
        } catch (DateTimeParseException ignore) {}

        try {
            return LocalDateTime.parse(raw, SPACE_DATETIME);
        } catch (DateTimeParseException ignore) {}

        return null;
    }

    private String firstText(Document doc, String... selectors) {
        for (String sel : selectors) {
            Element el = doc.selectFirst(sel);
            if (el != null) {
                String t = el.text();
                if (t != null && !t.isBlank()) return t.trim();
            }
        }
        return null;
    }

    private String firstAttr(Document doc, String attr, String... selectors) {
        for (String sel : selectors) {
            Element el = doc.selectFirst(sel);
            if (el != null) {
                String v = el.attr(attr);
                if (v != null && !v.isBlank()) return v.trim();
            }
        }
        return null;
    }

    private String attr(Document doc, String css, String attr) {
        Element el = doc.selectFirst(css);
        if (el == null) return null;
        String v = el.attr(attr);
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private void politeSleep() {
        int min = props.getSleepMinMs();
        int max = props.getSleepMaxMs();
        int sleep = min + new Random().nextInt(Math.max(1, max - min + 1));
        try { Thread.sleep(sleep); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String safe(String s) {
        return s == null ? "null" : s;
    }


    /**
     * 섹션 결과 반환용 (crawlSection() 리턴 타입)
     */
    public static class CrawlSectionResult {
        public final String section;
        public int scanned;
        public int saved;

        public int listFail;
        public int articleFail;
        public int parseFail;

        public int listPagesTried;
        public int listPagesSuccess;

        public CrawlSectionResult(String section) {
            this.section = section;
        }
    }

    /**
     * 전체 합산용 (crawlAllOnce() 내부에서만 사용)
     */
    private static class CrawlAggregate {
        int totalScanned;
        int totalSaved;
        int listFail;
        int articleFail;
        int parseFail;
        int allListFailedSections;

        void add(CrawlSectionResult r) {
            totalScanned += r.scanned;
            totalSaved += r.saved;
            listFail += r.listFail;
            articleFail += r.articleFail;
            parseFail += r.parseFail;

            if (r.listPagesTried > 0 && r.listPagesSuccess == 0) {
                allListFailedSections++;
            }
        }
    }

    private static class ParsedArticle {
        final String title;
        final String press;
        final LocalDateTime publishedAt;
        final String content;
        final String thumbnailUrl;

        ParsedArticle(String title, String press, LocalDateTime publishedAt, String content, String thumbnailUrl) {
            this.title = title;
            this.press = press;
            this.publishedAt = publishedAt;
            this.content = content;
            this.thumbnailUrl = thumbnailUrl;
        }
    }

    private static class ArticleId {
        final String oid;
        final String aid;

        ArticleId(String oid, String aid) {
            this.oid = oid;
            this.aid = aid;
        }

        String canonicalUrl() {
            return "https://n.news.naver.com/mnews/article/" + oid + "/" + aid;
        }

        static ArticleId from(String url) {
            if (url == null) return null;

            Matcher m = ARTICLE_PATH.matcher(url);
            if (m.find()) return new ArticleId(m.group(1), m.group(2));

            try {
                URI uri = URI.create(url);
                String q = uri.getQuery();
                if (q == null) return null;

                String oid = queryParam(q, "oid");
                String aid = queryParam(q, "aid");
                if (oid != null && aid != null) return new ArticleId(oid, aid);
            } catch (Exception ignore) {}

            return null;
        }

        private static String queryParam(String query, String key) {
            for (String part : query.split("&")) {
                int idx = part.indexOf('=');
                if (idx <= 0) continue;
                if (!part.substring(0, idx).equals(key)) continue;
                return part.substring(idx + 1);
            }
            return null;
        }
    }
}
