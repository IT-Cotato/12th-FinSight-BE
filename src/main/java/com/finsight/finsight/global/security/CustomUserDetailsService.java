package com.finsight.finsight.global.security;

import com.finsight.finsight.domain.user.domain.constant.AuthType;
import com.finsight.finsight.domain.user.persistence.entity.UserAuthEntity;
import com.finsight.finsight.domain.user.persistence.repository.UserAuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAuthRepository userAuthRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        UserAuthEntity userAuth = userAuthRepository
                .findByIdentifierAndAuthType(identifier, AuthType.EMAIL)
                .or(() -> userAuthRepository.findByIdentifierAndAuthType(identifier, AuthType.KAKAO))
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + identifier));

        return new CustomUserDetails(userAuth);
    }
}