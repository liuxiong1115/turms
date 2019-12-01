/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.turms.turms.config;

import com.fasterxml.classmate.TypeResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.schema.WildcardType;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebFlux;

import java.util.Arrays;
import java.util.List;

import static springfox.documentation.schema.AlternateTypeRules.newRule;

/**
 * https://springfox.github.io/springfox/docs/snapshot/
 */
@Configuration
@EnableSwagger2WebFlux
@Profile("dev")
public class SwaggerConfig {@Autowired
private TypeResolver typeResolver;

    @Bean
    public Docket docket() {
        List<Parameter> parameters = Arrays.asList(
                new ParameterBuilder()
                        .order(Ordered.HIGHEST_PRECEDENCE)
                        .name("account")
                        .modelRef(new ModelRef("string"))
                        .parameterType("header")
                        .required(true)
                        .defaultValue("turms")
                        .build(),
                new ParameterBuilder()
                        .order(Ordered.HIGHEST_PRECEDENCE + 1)
                        .name("password")
                        .modelRef(new ModelRef("string"))
                        .parameterType("header")
                        .required(true)
                        .defaultValue("turms")
                        .build());
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfoBuilder()
                        .title("Turms")
                        .version("0.9.0")
                        .build())
                .alternateTypeRules(newRule(typeResolver.resolve(Mono.class,
                        typeResolver.resolve(ResponseEntity.class, WildcardType.class)),
                        typeResolver.resolve(WildcardType.class)))
                .ignoredParameterTypes()
                // TODO: Exclude the APIs that don't need these headers
                //  Springfox don't support this for now) https://github.com/springfox/springfox/issues/1910
                .globalOperationParameters(parameters);
    }
}
