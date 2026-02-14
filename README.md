# 🌳 Git Flow 작업 가이드

`이슈 생성 → develop 최신화 → 브랜치 생성 → 작업 → PR (Squash & Merge)` 순서로 진행합니다.

### 1. 이슈 생성 (Issue)

- GitHub 레포지토리 `Issues` 탭에서 새로운 이슈를 생성하고, **이슈 번호**를 확인합니다.

### 2. develop 최신화

- **작업 시작 전**, 반드시 로컬의 `develop` 브랜치를 원격 저장소와 동기화합니다.
    
    ```jsx
    # develop 브랜치로 이동
    git checkout develop
    
    # 원격 저장소의 최신 내용 가져오기
    git pull origin develop
    ```
    

### 3. 브랜치 생성 (Create Branch)

- 최신화된 `develop`에서 작업할 새 브랜치를 생성합니다.
- **브랜치명 규칙:** `[타입]/#[이슈번호]-[작업내용]` (예: `feat/#12-login`)
    
    ```jsx
    git checkout -b [브랜치명]
    
    예시)
    git checkout -b feat/#12-login
    ```
    

### 4. 작업 및 커밋 (Commit)

- 작업을 진행하고 커밋 메시지 규칙(Convention)에 맞춰 커밋합니다.
    
    ```jsx
    git add .
    git commit -m "#12 :sparkles: feature: 로그인 기능 구현"
    ```
    

### 5. 푸시 (Push)

- 작업한 브랜치를 원격 저장소(GitHub)에 올립니다.Bash
    
    ```jsx
    git push origin [브랜치명]
    
    예시)
    git push origin feat/#12-login
    ```
    

### 6. PR 생성 및 머지 (Pull Request)

1. **PR 생성**: GitHub에서 `[내 브랜치]` ➡️ `develop` 으로 PR을 생성합니다.
2. **공유 및 리뷰**: PR 링크를 카톡방에 공유하고 팀원의 리뷰(또는 확인)를 받습니다.
3. **머지 (중요)**: 리뷰가 완료되면 **[Squash and Merge]** 버튼을 눌러 병합합니다.


# Commit message format

> 커밋 메세지의 기본 포멧은 아래의 명세를 따릅니다.
> 

```
#<issue_number>:Emoji: <type>: <subject>
ex: #1 :sparkles: feature: 로그인 기능 구현
```

## Type & Emoji


> 해당 커밋은 무엇에 대한 작업인지 키워드를 통해 표시합니다.
> 

| Type | Emoji | Description |
| --- | --- | --- |
| feat | ✨ (sparkles) | 새로운 기능 추가 |
| fix | 🐛 (bug) | 기존 기능 및 버그 수정 |
| docs | 📝 (memo) | 문서 수정 |
| style | 🎨 (art) | 코드 포맷팅, 세미콜론 누락, 코드 변경이 없는 경우 |
| refactor | ♻️ (recycle) | 코드 리팩토링(주석 제거도 포함) |
| test | ✅ (white_check_mark) | 테스트 코드, 리팩토링 테스트 코드 추가 |
| chore | 🔧 (wrench) | 빌드 업무 수정, 패키지 매니저 수정 |
| wip | 🚧 (construction) | 완료되지 않은 작업 임시 커밋
(가능하다면 지양합시다!) |
| rename | 🚚 (truck) | 파일 or 폴더명 수정하거나 옮기는 경우 |
