// Copyright 2019 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.analysis.config;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.analysis.PlatformOptions;
import com.google.devtools.build.lib.analysis.config.transitions.PatchTransition;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.events.StoredEventHandler;
import com.google.devtools.build.lib.packages.AttributeTransitionData;
import com.google.devtools.build.lib.testutil.FakeAttributeMapper;
import com.google.devtools.common.options.OptionsParsingException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ExecutionTransitionFactory}. */
@RunWith(JUnit4.class)
public class ExecutionTransitionFactoryTest {
  private static final Label EXECUTION_PLATFORM = Label.parseCanonicalUnchecked("//platform:exec");

  @Test
  public void executionTransition() throws OptionsParsingException, InterruptedException {
    PatchTransition transition =
        ExecutionTransitionFactory.createFactory()
            .create(
                AttributeTransitionData.builder()
                    .attributes(FakeAttributeMapper.empty())
                    .executionPlatform(EXECUTION_PLATFORM)
                    .build());

    assertThat(transition).isNotNull();

    // Apply the transition.
    BuildOptions options =
        BuildOptions.of(
            ImmutableList.of(CoreOptions.class, PlatformOptions.class),
            "--platforms=//platform:target");

    BuildOptions result =
        transition.patch(
            new BuildOptionsView(options, transition.requiresOptionFragments()),
            new StoredEventHandler());
    assertThat(result).isNotNull();
    assertThat(result).isNotSameInstanceAs(options);

    assertThat(result.contains(CoreOptions.class)).isNotNull();
    assertThat(result.get(CoreOptions.class).isExec).isTrue();
    assertThat(result.contains(PlatformOptions.class)).isNotNull();
    assertThat(result.get(PlatformOptions.class).platforms).containsExactly(EXECUTION_PLATFORM);
  }

  @Test
  public void executionTransition_noExecPlatform()
      throws OptionsParsingException, InterruptedException {
    // No execution platform available.
    PatchTransition transition =
        ExecutionTransitionFactory.createFactory()
            .create(
                AttributeTransitionData.builder()
                    .attributes(FakeAttributeMapper.empty())
                    .executionPlatform(null)
                    .build());

    assertThat(transition).isNotNull();

    // Apply the transition.
    BuildOptions options =
        BuildOptions.of(
            ImmutableList.of(CoreOptions.class, PlatformOptions.class),
            "--platforms=//platform:target");

    BuildOptions result =
        transition.patch(
            new BuildOptionsView(options, transition.requiresOptionFragments()),
            new StoredEventHandler());
    assertThat(result).isNotNull();
    assertThat(result).isEqualTo(options);
  }

  @Test
  public void executionTransition_confDist_legacy()
      throws OptionsParsingException, InterruptedException {
    PatchTransition transition =
        ExecutionTransitionFactory.createFactory()
            .create(
                AttributeTransitionData.builder()
                    .attributes(FakeAttributeMapper.empty())
                    .executionPlatform(EXECUTION_PLATFORM)
                    .build());

    assertThat(transition).isNotNull();

    // Apply the transition.
    BuildOptions options =
        BuildOptions.of(
            ImmutableList.of(CoreOptions.class, PlatformOptions.class),
            "--platforms=//platform:target",
            "--experimental_exec_configuration_distinguisher=legacy");

    BuildOptions result =
        transition.patch(
            new BuildOptionsView(options, transition.requiresOptionFragments()),
            new StoredEventHandler());

    assertThat(result.get(CoreOptions.class).affectedByStarlarkTransition).isEmpty();
    assertThat(result.get(CoreOptions.class).platformSuffix)
        .contains(String.format("%X", EXECUTION_PLATFORM.getCanonicalForm().hashCode()));
  }

  @Test
  public void executionTransition_confDist_fullHash()
      throws OptionsParsingException, InterruptedException {
    PatchTransition transition =
        ExecutionTransitionFactory.createFactory()
            .create(
                AttributeTransitionData.builder()
                    .attributes(FakeAttributeMapper.empty())
                    .executionPlatform(EXECUTION_PLATFORM)
                    .build());

    assertThat(transition).isNotNull();

    // Apply the transition.
    BuildOptions options =
        BuildOptions.of(
            ImmutableList.of(CoreOptions.class, PlatformOptions.class),
            "--platforms=//platform:target",
            "--experimental_exec_configuration_distinguisher=full_hash");

    BuildOptions result =
        transition.patch(
            new BuildOptionsView(options, transition.requiresOptionFragments()),
            new StoredEventHandler());

    BuildOptions mutableCopy = result.clone();
    mutableCopy.get(CoreOptions.class).platformSuffix = "";
    int fullHash = mutableCopy.hashCode();

    assertThat(result.get(CoreOptions.class).affectedByStarlarkTransition).isEmpty();
    assertThat(result.get(CoreOptions.class).platformSuffix)
        .contains(String.format("%X", fullHash));
  }

  @Test
  public void executionTransition_confDist_diffToAffected()
      throws OptionsParsingException, InterruptedException {
    PatchTransition transition =
        ExecutionTransitionFactory.createFactory()
            .create(
                AttributeTransitionData.builder()
                    .attributes(FakeAttributeMapper.empty())
                    .executionPlatform(EXECUTION_PLATFORM)
                    .build());

    assertThat(transition).isNotNull();

    // Apply the transition.
    BuildOptions options =
        BuildOptions.of(
            ImmutableList.of(CoreOptions.class, PlatformOptions.class),
            "--platforms=//platform:target",
            "--experimental_exec_configuration_distinguisher=diff_to_affected");

    BuildOptions result =
        transition.patch(
            new BuildOptionsView(options, transition.requiresOptionFragments()),
            new StoredEventHandler());

    assertThat(result.get(CoreOptions.class).affectedByStarlarkTransition).isNotEmpty();
    assertThat(result.get(CoreOptions.class).platformSuffix).isEqualTo("exec");
  }

  @Test
  public void executionTransition_confDist_off()
      throws OptionsParsingException, InterruptedException {
    PatchTransition transition =
        ExecutionTransitionFactory.createFactory()
            .create(
                AttributeTransitionData.builder()
                    .attributes(FakeAttributeMapper.empty())
                    .executionPlatform(EXECUTION_PLATFORM)
                    .build());

    assertThat(transition).isNotNull();

    // Apply the transition.
    BuildOptions options =
        BuildOptions.of(
            ImmutableList.of(CoreOptions.class, PlatformOptions.class),
            "--platforms=//platform:target",
            "--experimental_exec_configuration_distinguisher=off");

    BuildOptions result =
        transition.patch(
            new BuildOptionsView(options, transition.requiresOptionFragments()),
            new StoredEventHandler());

    assertThat(result.get(CoreOptions.class).affectedByStarlarkTransition).isEmpty();
    assertThat(result.get(CoreOptions.class).platformSuffix)
        .isEqualTo(options.get(CoreOptions.class).platformSuffix);
  }
}
