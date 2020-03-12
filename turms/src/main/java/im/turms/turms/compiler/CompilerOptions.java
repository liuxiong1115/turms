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

package im.turms.turms.compiler;

/**
 * Note that == should be used instead of equals() due to code optimization.
 */
public class CompilerOptions {

    public static class Env {
        private Env() {}
        public static final String DEV = "DEV";
        public static final String PROD = "PROD";
    }

    public static final String ENV = "@env@";

    private CompilerOptions() {}
}
