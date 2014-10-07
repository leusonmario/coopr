/*
 * Copyright © 2012-2014 Cask Data, Inc.
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
package co.cask.coopr.codec.json.current;

import co.cask.coopr.http.request.ClusterCreateRequest;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

/**
 * Codec for deserializing a {@link co.cask.coopr.http.request.ClusterCreateRequest}, used so some validation
 * is done on required fields.
 */
public class ClusterCreateRequestCodec implements JsonDeserializer<ClusterCreateRequest> {

  @Override
  public ClusterCreateRequest deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    return ClusterCreateRequest.builder()
      .setName(context.<String>deserialize(jsonObj.get("name"), String.class))
      .setDescription(context.<String>deserialize(jsonObj.get("description"), String.class))
      .setClusterTemplateName(context.<String>deserialize(jsonObj.get("clusterTemplate"), String.class))
      .setNumMachines(context.<Integer>deserialize(jsonObj.get("numMachines"), Integer.class))
      .setProviderName(context.<String>deserialize(jsonObj.get("provider"), String.class))
      .setHardwareTypeName(context.<String>deserialize(jsonObj.get("hardwaretype"), String.class))
      .setImageTypeName(context.<String>deserialize(jsonObj.get("imagetype"), String.class))
      .setInitialLeaseDuration(context.<Long>deserialize(jsonObj.get("initialLeaseDuration"), Long.class))
      .setDNSSuffix(context.<String>deserialize(jsonObj.get("dnsSuffix"), String.class))
      .setConfig(context.<JsonObject>deserialize(jsonObj.get("config"), JsonObject.class))
      .setServiceNames(context.<Set<String>>deserialize(jsonObj.get("services"),
                                                        new TypeToken<Set<String>>() {}.getType()))
      .setProviderFields(context.<Map<String, Object>>deserialize(jsonObj.get("providerFields"),
                                                                  new TypeToken<Map<String, Object>>() {}.getType()))
      .build();
  }
}
