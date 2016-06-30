/*
 *      Copyright 2016 The original authors.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.sundr.codegen.model;

import io.sundr.builder.Fluent;
import io.sundr.builder.Nested;
import io.sundr.codegen.model.*;
import io.sundr.codegen.model.Statement;
import io.sundr.codegen.model.StringStatement;
import io.sundr.codegen.model.StringStatementFluent;

import java.util.List;

public interface BlockFluent<A extends io.sundr.codegen.model.BlockFluent<A>> extends Fluent<A>{


    public A addToStatements(io.sundr.codegen.model.Statement... items);    public A removeFromStatements(io.sundr.codegen.model.Statement... items);    public List<io.sundr.codegen.model.Statement> getStatements();    public A withStatements(List<io.sundr.codegen.model.Statement> statements);    public A withStatements(Statement... statements);    public A addToStringStatementStatements(StringStatement... items);    public A removeFromStringStatementStatements(StringStatement... items);    public StringStatementStatementsNested<A> addNewStringStatementStatement();    public StringStatementStatementsNested<A> addNewStringStatementStatementLike(StringStatement item);    public A addNewStringStatementStatement(String data);
    public interface StringStatementStatementsNested<N> extends Nested<N>,StringStatementFluent<StringStatementStatementsNested<N>> {
            public N endStringStatementStatement();            public N and();        
}


}
