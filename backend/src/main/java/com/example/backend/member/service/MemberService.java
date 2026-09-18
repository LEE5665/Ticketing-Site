package com.example.backend.member.service;

import com.example.backend.member.entity.Member;
import com.example.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import com.example.backend.member.exception.DuplicateEmailException;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signup(String name, String email, String password) {
        email = email.trim().toLowerCase(Locale.ROOT);
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.");
        }
        if (memberRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        String encodedPassword = passwordEncoder.encode(password);

        Member member = new Member(name.trim(), email, encodedPassword);
        Member savedMember = memberRepository.save(member);

        return savedMember.getId();
    }
}
