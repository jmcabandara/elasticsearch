/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.repositories.s3;

import java.util.IdentityHashMap;

import com.amazonaws.services.s3.AmazonS3;
import org.elasticsearch.common.settings.Settings;

public class TestAwsS3Service extends S3Service {
    public static class TestPlugin extends S3RepositoryPlugin {
        public TestPlugin(Settings settings) {
            super(settings, new TestAwsS3Service(settings));
        }
    }

    IdentityHashMap<AmazonS3Reference, TestAmazonS3> clients = new IdentityHashMap<>();

    public TestAwsS3Service(Settings settings) {
        super(settings);
    }

    @Override
    public synchronized AmazonS3Reference client(String clientName) {
        return new AmazonS3Reference(cachedWrapper(super.client(clientName)));
    }

    private AmazonS3 cachedWrapper(AmazonS3Reference clientReference) {
        TestAmazonS3 wrapper = clients.get(clientReference);
        if (wrapper == null) {
            wrapper = new TestAmazonS3(clientReference.client(), settings);
            clients.put(clientReference, wrapper);
        }
        return wrapper;
    }

    @Override
    protected synchronized void releaseCachedClients() {
        super.releaseCachedClients();
        clients.clear();
    }

}
