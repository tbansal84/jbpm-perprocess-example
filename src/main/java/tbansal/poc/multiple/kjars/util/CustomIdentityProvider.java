/**
 * Copyright 2015, Red Hat, Inc.
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

package tbansal.poc.multiple.kjars.util;

import org.kie.internal.identity.IdentityProvider;

import java.util.ArrayList;
import java.util.List;

public class CustomIdentityProvider implements IdentityProvider {

    private List<String> roles = new ArrayList<String>();

    @Override
    public String getName() {
        return "system";
    }

    @Override
    public List<String> getRoles() {
        return roles;
    }

    @Override
    public boolean hasRole(String s) {
        return true;
    }
}
