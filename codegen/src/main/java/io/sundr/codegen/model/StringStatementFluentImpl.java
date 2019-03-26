/*
 *      Copyright 2019 The original authors.
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

import io.sundr.Provider;
import io.sundr.builder.BaseFluent;

public class StringStatementFluentImpl<A extends StringStatementFluent<A>> extends BaseFluent<A> implements StringStatementFluent<A>{

    private Provider<String> provider;

    public StringStatementFluentImpl(){
    }
    public StringStatementFluentImpl(StringStatement instance){
            this.withProvider(instance.getProvider()); 
    }

    public Provider<String> getProvider(){
            return this.provider;
    }

    public A withProvider(Provider<String> provider){
            this.provider=provider; return (A) this;
    }

    public Boolean hasProvider(){
            return this.provider != null;
    }

    public boolean equals(Object o){
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StringStatementFluentImpl that = (StringStatementFluentImpl) o;
            if (provider != null ? !provider.equals(that.provider) :that.provider != null) return false;
            return true;
    }




}
