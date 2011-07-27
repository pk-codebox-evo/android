/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.android.uipreview;

import com.android.ide.common.rendering.LayoutLibrary;
import com.android.ide.common.rendering.api.ILayoutPullParser;
import com.android.ide.common.rendering.api.IProjectCallback;
import com.android.ide.common.rendering.api.RenderSession;
import com.android.ide.common.rendering.api.SessionParams;
import com.android.ide.common.rendering.api.SessionParams.RenderingMode;
import com.android.ide.common.resources.ResourceResolver;
import com.android.ide.common.resources.configuration.*;
import com.android.resources.Density;
import com.android.resources.ScreenOrientation;
import com.android.resources.ScreenSize;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.StringReader;


/**
 * @author Eugene.Kudelevsky
 */
class RenderService {
  private static final Logger LOG = Logger.getInstance("#org.jetbrains.android.uipreview.RenderService");

  private final IProjectCallback myProjectCallback;
  private final ResourceResolver myResourceResolver;
  private final LayoutLibrary myLayoutLib;
  private final FolderConfiguration myConfig;
  private final int myMinSdkVersion;

  RenderService(LayoutLibrary layoutLibrary,
                @NotNull ResourceResolver resourceResolver,
                FolderConfiguration config,
                IProjectCallback projectCallback,
                int minSdkVersion) {
    myLayoutLib = layoutLibrary;
    myResourceResolver = resourceResolver;
    myConfig = config;
    myProjectCallback = projectCallback;
    myMinSdkVersion = minSdkVersion;
  }

  @Nullable
  @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
  public RenderSession createRenderSession(@NotNull String layoutXmlText, @NotNull String appLabel)
    throws FileNotFoundException, XmlPullParserException {

    final ILayoutPullParser parser = new XmlParser();
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
    parser.setInput(new StringReader(layoutXmlText));

    final Dimension dimension = getDimension();
    if (dimension == null) {
      return null;
    }

    final VersionQualifier versionQualifier = myConfig.getVersionQualifier();
    if (versionQualifier == null) {
      return null;
    }

    final int targetSdkVersion = versionQualifier.getVersion();
    final int minSdkVersion = myMinSdkVersion >= 0 ? myMinSdkVersion : targetSdkVersion;

    final PixelDensityQualifier densityQualifier = myConfig.getPixelDensityQualifier();
    final Density density = densityQualifier != null ? densityQualifier.getValue() : Density.MEDIUM;
    final int xdpi = density.getDpiValue();
    final int ydpi = density.getDpiValue();

    final SessionParams params =
      new SessionParams(parser, RenderingMode.NORMAL, this, dimension.width, dimension.height, density, xdpi, ydpi, myResourceResolver,
                        myProjectCallback, minSdkVersion, targetSdkVersion, new SimpleLogger(LOG));

    params.setExtendedViewInfoMode(false);
    params.setAppLabel(appLabel);

    final ScreenSizeQualifier screenSizeQualifier = myConfig.getScreenSizeQualifier();
    params.setConfigScreenSize(screenSizeQualifier != null ? screenSizeQualifier.getValue() : ScreenSize.NORMAL);
    return myLayoutLib.createSession(params);
  }

  @Nullable
  private Dimension getDimension() {
    final ScreenDimensionQualifier dimensionQualifier = myConfig.getScreenDimensionQualifier();

    final int size1 = dimensionQualifier != null ? dimensionQualifier.getValue1() : 320;
    final int size2 = dimensionQualifier != null ? dimensionQualifier.getValue2() : 240;

    final ScreenOrientationQualifier orientationQualifier = myConfig.getScreenOrientationQualifier();

    final ScreenOrientation orientation = orientationQualifier != null
                                          ? orientationQualifier.getValue()
                                          : ScreenOrientation.PORTRAIT;

    switch (orientation) {
      case LANDSCAPE:
        return new Dimension(size1 < size2 ? size2 : size1, size1 < size2 ? size1 : size2);
      case PORTRAIT:
        return new Dimension(size1 < size2 ? size1 : size2, size1 < size2 ? size2 : size1);
      case SQUARE:
        return new Dimension(size1, size1);
      default:
        LOG.error("Unknown screen orientation " + orientation);
        return null;
    }
  }
}
