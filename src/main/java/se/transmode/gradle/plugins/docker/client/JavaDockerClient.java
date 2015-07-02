/**
 * Copyright 2014 Transmode AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.transmode.gradle.plugins.docker.client;

import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.api.model.EventStreamItem;
import com.github.dockerjava.api.model.Identifier;
import com.github.dockerjava.api.model.PushEventStreamItem;
import com.github.dockerjava.api.model.Repository;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;
import java.io.IOException;

public class JavaDockerClient implements DockerClient {

    private static Logger log = Logging.getLogger(JavaDockerClient.class);
    private com.github.dockerjava.api.DockerClient client;

    JavaDockerClient(String url) {
        client = com.github.dockerjava.core.DockerClientImpl.getInstance(url);
    }

    JavaDockerClient(DockerClientConfig config) {
        client = DockerClientBuilder.getInstance(config).build();
    }

    @Override
    public void buildImage(File buildDir, String tag) {
        Preconditions.checkNotNull(tag, "Image tag can not be null.");
        Preconditions.checkArgument(!tag.isEmpty(),  "Image tag can not be empty.");
        BuildImageCmd.Response response = buildImageCmd(buildDir).withTag(tag).exec();
        checkResponse(response);
    }

    private BuildImageCmd buildImageCmd(File buildDir) {
        return client.buildImageCmd(buildDir);
    }

    @Override
    public void pushImage(String repository,String tag) {
        Preconditions.checkNotNull(repository, "Image repository can not be null.");
        Preconditions.checkNotNull(tag, "Image tag can not be null.");
        Preconditions.checkArgument(!tag.isEmpty(),  "Image tag can not be empty.");
        PushImageCmd.Response response = pushImageCmd(repository,tag).exec();
        checkResponse(response);
    }

    private PushImageCmd pushImageCmd(String repository,String tag) {
        return client.pushImageCmd(new Identifier(new Repository(repository), tag));
    }

    @Override
    public void tagImage(String imageID,String repository,String tag){
        Preconditions.checkNotNull(imageID, "Image imageID can not be null.");
        Preconditions.checkNotNull(repository, "Image repository can not be null.");
        Preconditions.checkNotNull(tag, "Image tag can not be null.");
        Preconditions.checkArgument(!tag.isEmpty(),  "Image tag can not be empty.");
        client.tagImageCmd(imageID,repository,tag).withForce().exec();
    }

    private static void checkResponse(BuildImageCmd.Response response) {
        try {
            Iterable<EventStreamItem> items = response.getItems();
            for(EventStreamItem item:items){
                if(item.getError() == null){
                    log.info(item.getStream());
                }else{
                    throw new GradleException("Docker API error:"+item.getErrorDetail());
                }
            }
        }catch (IOException e){
            throw new GradleException(
                    "Docker API error: Failed to build Image:\n" + e.getMessage(),e);
        }
    }

    private static void checkResponse(PushImageCmd.Response response) {
        try {
            Iterable<PushEventStreamItem> items = response.getItems();
            for(PushEventStreamItem item:items){
                log.info(item.toString());
            }
        }catch (IOException e){
            throw new GradleException(
                    "Docker API error: Failed to build Image:\n" + e.getMessage(),e);
        }
    }


    public static JavaDockerClient create(String url, String serverAddress, String user, String password, String email) {

        if (StringUtils.isEmpty(url)) {
            log.info("Connecting to localhost");
            url = "http://localhost:2375";
        } else {
            log.info("Connecting to {}", url);
        }
        DockerClientConfig.DockerClientConfigBuilder configBuilder = DockerClientConfig.createDefaultConfigBuilder()
                .withUri(url);
        if(user != null){
            configBuilder.withUsername(user);
        }
        if(password != null){
            configBuilder.withPassword(password);
        }
        if(email != null){
            configBuilder.withEmail(email);
        }
        if(serverAddress != null){
            configBuilder.withServerAddress(serverAddress);
        }
        JavaDockerClient client = new JavaDockerClient(configBuilder.build());
        return client;
    }
}
