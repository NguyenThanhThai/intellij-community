package com.intellij.ide.util.newProjectWizard.impl;

import com.intellij.ide.util.frameworkSupport.*;
import com.intellij.ide.util.newProjectWizard.*;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportConfigurable;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author nik
 */
public class FrameworkSupportModelImpl extends UserDataHolderBase implements FrameworkSupportModel {
  private final Project myProject;
  private final ModuleBuilder myModuleBuilder;
  private final EventDispatcher<FrameworkSupportModelListener> myDispatcher = EventDispatcher.create(FrameworkSupportModelListener.class);
  private final Map<String, FrameworkSupportNode> mySettingsMap = new HashMap<String, FrameworkSupportNode>();

  public FrameworkSupportModelImpl(final @Nullable Project project, @Nullable ModuleBuilder builder) {
    myProject = project;
    myModuleBuilder = builder;
  }

  public void registerComponent(@NotNull final FrameworkSupportProvider provider, @NotNull final FrameworkSupportNode node) {
    mySettingsMap.put(provider.getId(), node);
  }

  public Project getProject() {
    return myProject;
  }

  public ModuleBuilder getModuleBuilder() {
    return myModuleBuilder;
  }

  public boolean isFrameworkSelected(@NotNull @NonNls final String providerId) {
    final FrameworkSupportNode node = mySettingsMap.get(providerId);
    return node != null && node.isChecked();
  }

  public void addFrameworkListener(@NotNull final FrameworkSupportModelListener listener) {
    myDispatcher.addListener(listener);
  }

  public void removeFrameworkListener(@NotNull final FrameworkSupportModelListener listener) {
    myDispatcher.removeListener(listener);
  }

  public void setFrameworkComponentEnabled(@NotNull @NonNls final String providerId, final boolean enable) {
    final FrameworkSupportNode node = mySettingsMap.get(providerId);
    if (node == null) {
      throw new IllegalArgumentException("provider '" + providerId + " not found");
    }
    if (enable != node.isChecked()) {
      node.setChecked(enable);
    }
  }

  public FrameworkSupportConfigurable getFrameworkConfigurable(@NotNull @NonNls String providerId) {
    final FrameworkSupportNode node = mySettingsMap.get(providerId);
    if (node == null) {
      throw new IllegalArgumentException("provider '" + providerId + " not found");
    }
    return node.getConfigurable();
  }

  public void onFrameworkSelectionChanged(FrameworkSupportNode node) {
    final FrameworkSupportModelListener multicaster = myDispatcher.getMulticaster();
    if (node.isChecked()) {
      multicaster.frameworkSelected(node.getProvider());
    }
    else {
      multicaster.frameworkUnselected(node.getProvider());
    }
  }
}
