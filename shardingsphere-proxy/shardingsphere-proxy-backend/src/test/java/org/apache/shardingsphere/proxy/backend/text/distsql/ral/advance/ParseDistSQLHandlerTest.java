/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.proxy.backend.text.distsql.ral.advance;

import com.google.gson.Gson;
import org.apache.shardingsphere.distsql.parser.statement.ral.advanced.ParseStatement;
import org.apache.shardingsphere.infra.database.type.dialect.MySQLDatabaseType;
import org.apache.shardingsphere.infra.metadata.database.rule.ShardingSphereRuleMetaData;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.parser.rule.SQLParserRule;
import org.apache.shardingsphere.parser.rule.builder.DefaultSQLParserRuleConfigurationBuilder;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.proxy.backend.session.ConnectionSession;
import org.apache.shardingsphere.proxy.backend.text.distsql.ral.advanced.ParseDistSQLHandler;
import org.apache.shardingsphere.proxy.backend.util.ProxyContextRestorer;
import org.apache.shardingsphere.sql.parser.exception.SQLParsingException;
import org.apache.shardingsphere.sql.parser.sql.common.statement.SQLStatement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.SQLException;
import java.util.LinkedList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class ParseDistSQLHandlerTest extends ProxyContextRestorer {
    
    private final SQLParserRule sqlParserRule = new SQLParserRule(new DefaultSQLParserRuleConfigurationBuilder().build());
    
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ContextManager contextManager;
    
    @Mock
    private ConnectionSession connectionSession;
    
    @Before
    public void setUp() throws SQLException {
        ShardingSphereRuleMetaData globalRuleMetaData = mock(ShardingSphereRuleMetaData.class);
        when(contextManager.getMetaDataContexts().getMetaData().getGlobalRuleMetaData()).thenReturn(globalRuleMetaData);
        when(globalRuleMetaData.getSingleRule(SQLParserRule.class)).thenReturn(sqlParserRule);
        when(connectionSession.getDatabaseType()).thenReturn(new MySQLDatabaseType());
        ProxyContext.init(contextManager);
    }
    
    @Test
    public void assertGetRowData() throws SQLException {
        String sql = "select * from t_order";
        ParseStatement parseStatement = new ParseStatement(sql);
        ParseDistSQLHandler parseDistSQLHandler = new ParseDistSQLHandler();
        parseDistSQLHandler.init(parseStatement, connectionSession);
        parseDistSQLHandler.execute();
        parseDistSQLHandler.next();
        SQLStatement statement = sqlParserRule.getSQLParserEngine("MySQL").parse(sql, false);
        assertThat(new LinkedList<>(parseDistSQLHandler.getRowData()).getFirst(), is("MySQLSelectStatement"));
        assertThat(new LinkedList<>(parseDistSQLHandler.getRowData()).getLast(), is(new Gson().toJson(statement)));
    }
    
    @Test(expected = SQLParsingException.class)
    public void assertExecute() throws SQLException {
        String sql = "wrong sql";
        ParseStatement parseStatement = new ParseStatement(sql);
        ParseDistSQLHandler parseDistSQLHandler = new ParseDistSQLHandler();
        parseDistSQLHandler.init(parseStatement, connectionSession);
        parseDistSQLHandler.execute();
    }
}
