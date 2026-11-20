package com.example.slackclone.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.example.slackclone")
class LayerDependencyTest {

    // 왜: 핵심 로직이 웹 기술에 묶이면 테스트와 교체가 어려워진다. 규칙 위반은 빌드 실패로 막는다.
    // 어디: 아직 domain·service에 클래스가 없어 allowEmptyShould를 켠다. 클래스가 생기면 자동으로 검사된다.
    @ArchTest
    static final ArchRule domain_service는_Spring_Web을_모른다 =
            noClasses().that().resideInAnyPackage(
                            "com.example.slackclone.domain..",
                            "com.example.slackclone.service..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.web..",
                            "org.springframework.http..")
                    .allowEmptyShould(true);
}
