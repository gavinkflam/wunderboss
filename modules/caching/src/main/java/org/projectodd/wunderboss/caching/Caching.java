/*
 * Copyright 2014 Red Hat, Inc, and individual contributors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.projectodd.wunderboss.caching;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.projectodd.wunderboss.Component;
import org.projectodd.wunderboss.Option;

public interface Caching extends Component {
    class CreateOption extends Option {
        public static final CreateOption CONFIGURATION = opt("configuration", CreateOption.class);
        // transactional
        // locking
        // sync
        // encoding
        // name
        // persist
        // clustering mode
        // eviction
        // max entries
        // idle
        // ttl
    }
    Cache create(String name, Configuration configuration);
    Cache find(String name);
    Cache findOrCreate(String name, Configuration configuration);
}