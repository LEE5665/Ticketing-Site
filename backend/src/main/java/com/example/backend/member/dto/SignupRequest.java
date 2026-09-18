package com.example.backend.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 100, message = "이름은 100자 이하로 입력해 주세요.") String name,
    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "올바른 이메일을 입력해 주세요.")
    @Size(max = 254, message = "이메일이 너무 깁니다.") String email,
    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하로 입력해 주세요.") String password
) {
}
