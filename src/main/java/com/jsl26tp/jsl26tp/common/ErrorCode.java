package com.jsl26tp.jsl26tp.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ===== 인증/인가 (AUTH) =====
    UNAUTHORIZED          (HttpStatus.UNAUTHORIZED,  "ログインが必要です。"),
    LOGIN_FAILED          (HttpStatus.UNAUTHORIZED,  "IDまたはパスワードが正しくありません。"),
    ACCESS_DENIED         (HttpStatus.FORBIDDEN,     "アクセス権限がありません。"),
    ACCOUNT_SUSPENDED     (HttpStatus.FORBIDDEN,     "アカウントが停止されています。"),
    ACCOUNT_BANNED        (HttpStatus.FORBIDDEN,     "アカウントがブロックされています。"),

    // ===== 회원 (USER) =====
    USER_NOT_FOUND        (HttpStatus.NOT_FOUND,     "ユーザーが見つかりません。"),
    DUPLICATE_USERNAME    (HttpStatus.CONFLICT,      "すでに使用されているIDです。"),
    DUPLICATE_NICKNAME    (HttpStatus.CONFLICT,      "すでに使用されているニックネームです。"),
    DUPLICATE_EMAIL       (HttpStatus.CONFLICT,      "すでに使用されているメールアドレスです。"),
    INVALID_PASSWORD      (HttpStatus.BAD_REQUEST,   "パスワードは8文字以上で入力してください。"),
    PASSWORD_MISMATCH     (HttpStatus.BAD_REQUEST,   "パスワードが一致しません。"),

    // ===== 화장실 (TOILET) =====
    TOILET_NOT_FOUND      (HttpStatus.NOT_FOUND,     "トイレ情報が見つかりません。"),

    // ===== 리뷰 (REVIEW) =====
    REVIEW_NOT_FOUND      (HttpStatus.NOT_FOUND,     "レビューが見つかりません。"),
    REVIEW_NOT_OWNER      (HttpStatus.FORBIDDEN,     "自分のレビューのみ修正できます。"),

    // ===== 신고 (REPORT) =====
    REPORT_NOT_FOUND      (HttpStatus.NOT_FOUND,     "通報情報が見つかりません。"),
    DUPLICATE_REPORT      (HttpStatus.CONFLICT,      "すでに通報済みです。"),

    // ===== 문의 (INQUIRY) =====
    INQUIRY_NOT_FOUND     (HttpStatus.NOT_FOUND,     "お問い合わせが見つかりません。"),

    // ===== 파일 (FILE) =====
    FILE_UPLOAD_FAILED    (HttpStatus.INTERNAL_SERVER_ERROR, "ファイルのアップロードに失敗しました。"),

    // ===== 공통 (COMMON) =====
    BAD_REQUEST           (HttpStatus.BAD_REQUEST,   "リクエストが正しくありません。"),
    INTERNAL_ERROR        (HttpStatus.INTERNAL_SERVER_ERROR, "サーバーエラーが発生しました。");

    private final HttpStatus status;
    private final String message;
}
