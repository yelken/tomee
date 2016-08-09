/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.tomee.jdbc;

import org.apache.openejb.jee.EjbJar;
import org.apache.openejb.junit.ApplicationComposer;
import org.apache.openejb.testing.Configuration;
import org.apache.openejb.testing.Module;
import org.apache.openejb.testng.PropertiesBuilder;
import org.hsqldb.jdbc.pool.JDBCXAConnectionWrapper;
import org.hsqldb.jdbc.pool.JDBCXADataSource;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(ApplicationComposer.class)
public class TomcatXADataSourceTest {
    private static final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    @Resource(name = "xadb")
    private DataSource ds;

    @Module
    public EjbJar mandatory() {
        return new EjbJar();
    }

    @Configuration
    public Properties props() {
        return new PropertiesBuilder()
            .p("openejb.jdbc.datasource-creator", TomEEDataSourceCreator.class.getName())

            .p("txMgr", "new://TransactionManager?type=TransactionManager")
            .p("txMgr.txRecovery", "true")
            .p("txMgr.logFileDir", "target/test/xa/howl")

                // real XA datasources
            .p("xa", "new://Resource?class-name=" + JDBCXADataSource.class.getName())
            .p("xa.url", "jdbc:hsqldb:mem:tomcat-xa")
            .p("xa.user", "sa")
            .p("xa.password", "")
            .p("xa.SkipImplicitAttributes", "true")

            .p("xadb", "new://Resource?type=DataSource")
            .p("xadb.xaDataSource", "xa")
            .p("xadb.JtaManaged", "true")

            .build();
    }

    @Test
    public void check() throws Exception {
        assertNotNull(ds);
        final Connection c = ds.getConnection();
        assertNotNull(c);
        assertThat(c.getMetaData().getConnection(), instanceOf(JDBCXAConnectionWrapper.class));
        c.close();

        assertEquals(0, getActiveConnections("xadb"));
    }


    private int getActiveConnections(final String dataSourceName)
            throws MalformedObjectNameException, MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        final ObjectName objectName = new ObjectName("openejb.management:ObjectType=datasources,DataSource=" + dataSourceName);
        final Object activeConnectionsAttribute = server.getAttribute(objectName, "Active");
        return (int) (Integer) activeConnectionsAttribute;
    }
}
