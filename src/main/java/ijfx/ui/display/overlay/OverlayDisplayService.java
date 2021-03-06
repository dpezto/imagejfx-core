/*
    This file is part of ImageJ FX.

    ImageJ FX is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ImageJ FX is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ImageJ FX.  If not, see <http://www.gnu.org/licenses/>. 
    
     Copyright 2015,2016 Cyril MONGIS, Michael Knop
	
 */
package ijfx.ui.display.overlay;

import ijfx.core.IjfxService;
import ijfx.ui.display.image.CanvasListener;
import ijfx.ui.main.ImageJFX;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import net.imagej.display.ImageDisplay;
import net.imagej.display.OverlayService;
import net.imagej.overlay.Overlay;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;
import org.scijava.service.SciJavaService;
import org.scijava.util.IntCoords;
import org.scijava.util.RealCoords;

/**
 *
 * @author Cyril MONGIS, 2016
 */
@Plugin(type = SciJavaService.class)
public class OverlayDisplayService extends AbstractService implements IjfxService {

    //HashMap<Overlay, OverlayDrawer> drawerMap = new HashMap<>();
    //HashMap<Overlay, OverlayModifier> modifierMap = new HashMap<>();
    @Parameter
    PluginService pluginService;

    @Parameter
    Context context;

    @Parameter
    OverlayService overlayService;
    
    Logger logger = ImageJFX.getLogger();
    
    private final Map<Class<? extends Overlay>, OverlayDrawer> drawerMap = new HashMap<>();

    public OverlayModifier createModifier(Overlay overlay) {
        return findPluginFor(OverlayModifier.class, overlay);
    }

    //public OverlayDrawer createDrawer(Overlay overlay) {
    //  return findPluginFor(OverlayDrawer.class, overlay);
    //}
    public OverlayDrawer createDrawer(Class<?> o) {

        for (OverlayDrawer drawer : pluginService.createInstancesOfType(OverlayDrawer.class)) {
            if (drawer.canHandle(o)) {
                return drawer;
            }
        }
        return null;
    }

    private <T extends ClassHandler, C> T findPluginFor(Class<? extends T> handlerType, C overlay) {

        // if (map.containsKey(overlay) == false || map.get(overlay) == null) {
        for (T plugin : pluginService.createInstancesOfType(handlerType)) {
            if (plugin.canHandle(overlay)) {

                return plugin;
                // map.put(overlay, plugin);

            }
        }
        // }

        return null;
    }

    public OverlayDrawer getDrawer(Overlay overlay) {
        //logger.info("Searching a drawer for "+overlay.getClass().getSimpleName());
        if (drawerMap.get(overlay.getClass()) == null) {
            OverlayDrawer drawer = createDrawer(overlay.getClass());
            if (drawer == null) {
                logger.warning("No overlay compatible for " + overlay.getClass().getSimpleName());
                return null;
            } else {
                drawerMap.put(overlay.getClass(), drawer);
                return drawer;
            }
        } else {
            return drawerMap.get(overlay.getClass());
        }
    }
    
    public boolean isOnOverlay(ImageDisplay display, double xOnCanvas, double yOnCanvas, Overlay overlay) {
         OverlayDrawer drawer = getDrawer(overlay);

        if (drawer == null) {
            return false;
        }

        RealCoords onData = display.getCanvas().panelToDataCoords(new IntCoords(toInt(xOnCanvas), toInt(yOnCanvas)));

        boolean result = drawer.isOnOverlay(overlay, onData.x, onData.y);
        return result;
    }
    
    
    public Overlay findOverlay(ImageDisplay display, double xOnCanvas, double yOnCanvas) {
        
       return overlayService
               .getOverlays(display)
               .stream()
               .filter(overlay->isOnOverlay(display, xOnCanvas, yOnCanvas, overlay))
               .findFirst()
               .orElse(null);
        
    }
    
    private int toInt(double d) {
        return CanvasListener.toInt(d);
    }

}
