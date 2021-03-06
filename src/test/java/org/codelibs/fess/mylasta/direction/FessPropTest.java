/*
 * Copyright 2012-2016 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.mylasta.direction;

import java.io.File;
import java.io.IOException;

import org.codelibs.core.io.FileUtil;
import org.codelibs.core.misc.DynamicProperties;
import org.codelibs.fess.unit.UnitFessTestCase;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;

public class FessPropTest extends UnitFessTestCase {

    @Override
    protected boolean isUseOneTimeContainer() {
        return true;
    }

    public void test_maxUsernameLength() throws IOException {
        FessConfig fessConfig = new FessConfig.SimpleImpl() {
            @Override
            public Integer getLdapMaxUsernameLengthAsInteger() {
                return Integer.valueOf(-1);
            }
        };
        File file = File.createTempFile("test", ".properties");
        file.deleteOnExit();
        FileUtil.writeBytes(file.getAbsolutePath(), "ldap.security.principal=%s@fess.codelibs.local".getBytes("UTF-8"));
        DynamicProperties systemProps = new DynamicProperties(file);
        SingletonLaContainerFactory.getContainer().register(systemProps, "systemProperties");

        assertEquals("@fess.codelibs.local", fessConfig.getLdapSecurityPrincipal(null));
        assertEquals("@fess.codelibs.local", fessConfig.getLdapSecurityPrincipal(""));
        assertEquals("123456789@fess.codelibs.local", fessConfig.getLdapSecurityPrincipal("123456789"));
        assertEquals("1234567890@fess.codelibs.local", fessConfig.getLdapSecurityPrincipal("1234567890"));
        assertEquals("12345678901@fess.codelibs.local", fessConfig.getLdapSecurityPrincipal("12345678901"));
    }

    public void test_maxUsernameLength10() throws IOException {
        FessConfig fessConfig = new FessConfig.SimpleImpl() {
            @Override
            public Integer getLdapMaxUsernameLengthAsInteger() {
                return Integer.valueOf(10);
            }
        };

        File file = File.createTempFile("test", ".properties");
        file.deleteOnExit();
        FileUtil.writeBytes(file.getAbsolutePath(), "ldap.security.principal=%s@fess.codelibs.local".getBytes("UTF-8"));
        DynamicProperties systemProps = new DynamicProperties(file);
        SingletonLaContainerFactory.getContainer().register(systemProps, "systemProperties");

        assertEquals("@fess.codelibs.local", fessConfig.getLdapSecurityPrincipal(null));
        assertEquals("@fess.codelibs.local", fessConfig.getLdapSecurityPrincipal(""));
        assertEquals("123456789@fess.codelibs.local", fessConfig.getLdapSecurityPrincipal("123456789"));
        assertEquals("1234567890@fess.codelibs.local", fessConfig.getLdapSecurityPrincipal("1234567890"));
        assertEquals("1234567890@fess.codelibs.local", fessConfig.getLdapSecurityPrincipal("12345678901"));
    }

}
