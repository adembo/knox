/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.gateway.filter.rewrite.impl.form;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.hadoop.gateway.filter.rewrite.api.UrlRewriter;
import org.apache.hadoop.gateway.filter.rewrite.spi.UrlRewriteStreamFilter;
import org.apache.hadoop.gateway.util.urltemplate.Resolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FormUrlRewriteStreamFilter implements UrlRewriteStreamFilter {

  private static String[] TYPES = new String[]{ "application/x-www-form-urlencoded", "*/x-www-form-urlencoded" };
  private static String[] NAMES = new String[]{ null };

  @Override
  public String[] getTypes() {
    return TYPES;
  }

  @Override
  public String[] getNames() {
    return NAMES;
  }

  @Override
  public InputStream filter(
      InputStream stream,
      String encoding,
      UrlRewriter rewriter,
      Resolver resolver,
      UrlRewriter.Direction direction )
          throws IOException {
    return new ReaderInputStream(
        new FormUrlRewriteFilterReader(
            new InputStreamReader( stream, encoding ), rewriter, resolver, direction ) );
  }

}
