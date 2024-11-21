/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Apache UIMA
 * <p>
 * This package contains:
 * </p>
 * <ul>
 * <li>The {@link org.apache.uima.UIMAFramework} class provides the primary interface point for
 * applications.</li>
 * <li>Most of the error / exception indirection classes (to allow IDEs to do auto-complete looking
 * for messages).</li>
 * </ul>
 * <h2>Internationalized Exceptions and Messages</h2>
 * <p>
 * Internationalization is handled by the static methods in I18nUtil. &nbsp;These are called by the
 * Internationalized Exceptions, but may be also used for non-exception message localization.
 * </p>
 * <p>
 * Exception messages are collected into classes. &nbsp;These classes may be organized further into
 * hierarchies, but the top of these extend one of the following 3 classes:
 * </p>
 * <ul>
 * <li>Exception - for checked exceptions</li>
 * <li>RuntimeException - for unchecked exceptions</li>
 * <li>SaxException - for exceptions thrown during Sax related callbacks requiring SaxExceptions be
 * thrown</li>
 * </ul>
 * <p>
 * Common code for getting a localized message from arguments and message key and resource bundle
 * are put in the interface I18nExceptionI as default methods. Using default methods allows shared
 * methods to be used with different superclass chains.
 * </p>
 * <p>
 * The individual classes:
 * </p>
 * <ul>
 * <li>hold static public MSG_NAME = "prop-file-key-name" values, allow IDE search via completion,
 * allows renaming via Eclipse refactorization</li>
 * <li>Classes collect messages for some sub-section of the code</li>
 * <li>Super class structure can supply common resource bundles</li>
 * </ul>
 */
package org.apache.uima;