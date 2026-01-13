package com.rebirth.my.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OotdTableInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> OOTD 테이블 초기화 로직 시작...");

        if (!checkTableExists("OOTD_POSTS")) {
            System.out.println(">>> OOTD_POSTS 테이블이 없어 생성을 시도합니다.");
            createPostTable();
        } else {
            System.out.println(">>> OOTD_POSTS 테이블이 이미 존재합니다.");
        }

        if (!checkTableExists("OOTD_LIKES")) {
            System.out.println(">>> OOTD_LIKES 테이블이 없어 생성을 시도합니다.");
            createLikeTable();
        }

        if (!checkTableExists("OOTD_COMMENTS")) {
            System.out.println(">>> OOTD_COMMENTS 테이블이 없어 생성을 시도합니다.");
            createCommentTable();
        }

        System.out.println(">>> OOTD 테이블 초기화 완료.");
    }

    private boolean checkTableExists(String tableName) {
        try {
            jdbcTemplate.queryForObject("SELECT count(*) FROM " + tableName + " WHERE 1=0", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void createPostTable() {
        try {
            // 시퀀스 생성 (존재시 무시)
            try {
                jdbcTemplate.execute("CREATE SEQUENCE OOTD_SEQ START WITH 1 INCREMENT BY 1 NOCACHE");
            } catch (Exception e) {
            }

            // 테이블 생성 - FK: USERS
            jdbcTemplate.execute("""
                        CREATE TABLE OOTD_POSTS (
                            OOTD_ID NUMBER PRIMARY KEY,
                            USER_ID NUMBER NOT NULL,
                            TITLE VARCHAR2(200) NOT NULL,
                            DESCRIPTION VARCHAR2(1000),
                            IMAGE_URL VARCHAR2(500),
                            TOP_CLOTHES_ID VARCHAR2(50),
                            BOTTOM_CLOTHES_ID VARCHAR2(50),
                            TOP_TYPE VARCHAR2(50),
                            BOTTOM_TYPE VARCHAR2(50),
                            TOP_COLOR VARCHAR2(50),
                            BOTTOM_COLOR VARCHAR2(50),
                            SHOE_COLOR VARCHAR2(50),
                            BACKGROUND_THEME VARCHAR2(50),
                            TAGS VARCHAR2(500),
                            VIEW_COUNT NUMBER DEFAULT 0,
                            CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT FK_OOTD_USER FOREIGN KEY (USER_ID) REFERENCES USERS(ID) ON DELETE CASCADE
                        )
                    """);
            System.out.println(">>> ✅ OOTD_POSTS 테이블 생성 완료");
        } catch (Exception e) {
            System.err.println(">>> ❌ OOTD_POSTS 생성 실패: " + e.getMessage());
            // FK 문제일 경우 FK 없이 재시도
            if (e.getMessage().contains("ORA-00942") || e.getMessage().contains("grammar")) {
                System.out.println(">>> 외래키 제약조건 제거 후 재시도합니다...");
                try {
                    jdbcTemplate.execute("""
                                CREATE TABLE OOTD_POSTS (
                                    OOTD_ID NUMBER PRIMARY KEY,
                                    USER_ID NUMBER NOT NULL,
                                    TITLE VARCHAR2(200) NOT NULL,
                                    DESCRIPTION VARCHAR2(1000),
                                    IMAGE_URL VARCHAR2(500),
                                    TOP_CLOTHES_ID VARCHAR2(50),
                                    BOTTOM_CLOTHES_ID VARCHAR2(50),
                                    TOP_TYPE VARCHAR2(50),
                                    BOTTOM_TYPE VARCHAR2(50),
                                    TOP_COLOR VARCHAR2(50),
                                    BOTTOM_COLOR VARCHAR2(50),
                                    SHOE_COLOR VARCHAR2(50),
                                    BACKGROUND_THEME VARCHAR2(50),
                                    TAGS VARCHAR2(500),
                                    VIEW_COUNT NUMBER DEFAULT 0,
                                    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                                )
                            """);
                    System.out.println(">>> ✅ OOTD_POSTS 테이블 생성 완료 (FK 없음)");
                } catch (Exception ex) {
                    System.err.println(">>> ❌ 재시도 실패: " + ex.getMessage());
                }
            }
        }
    }

    private void createLikeTable() {
        try {
            jdbcTemplate.execute("""
                        CREATE TABLE OOTD_LIKES (
                            OOTD_ID NUMBER NOT NULL,
                            USER_ID NUMBER NOT NULL,
                            CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (OOTD_ID, USER_ID)
                        )
                    """);
            System.out.println(">>> ✅ OOTD_LIKES 테이블 생성 완료");
        } catch (Exception e) {
            System.err.println(">>> ❌ OOTD_LIKES 생성 실패: " + e.getMessage());
        }
    }

    private void createCommentTable() {
        try {
            try {
                jdbcTemplate.execute("CREATE SEQUENCE OOTD_COMMENT_SEQ START WITH 1 INCREMENT BY 1 NOCACHE");
            } catch (Exception e) {
            }

            jdbcTemplate.execute("""
                        CREATE TABLE OOTD_COMMENTS (
                            COMMENT_ID NUMBER PRIMARY KEY,
                            OOTD_ID NUMBER NOT NULL,
                            USER_ID NUMBER NOT NULL,
                            CONTENT VARCHAR2(1000) NOT NULL,
                            CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);
            System.out.println(">>> ✅ OOTD_COMMENTS 테이블 생성 완료");
        } catch (Exception e) {
            System.err.println(">>> ❌ OOTD_COMMENTS 생성 실패: " + e.getMessage());
        }
    }
}
