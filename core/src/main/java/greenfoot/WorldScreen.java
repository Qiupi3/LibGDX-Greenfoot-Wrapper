/*
 This file is part of the Greenfoot program.
 Copyright (C) 2005-2009,2010,2011,2013,2014,2015,2016,2021 Poul Henriksen and Michael Kolling

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 This file is subject to the Classpath exception as provided in the
 LICENSE file that accompanied this code.
*/

package greenfoot;

import com.badlogic.gdx.Screen;

/**
 * Adapts a {@link World} to LibGDX's {@link Screen} lifecycle.
 *
 * World deliberately does not implement Screen itself. Greenfoot projects commonly
 * give their world classes methods named show(), hide(), pause(), resume(), render()
 * or dispose() - Greenfoot's own World has no such methods, so the names are free -
 * and if World implemented Screen those user methods would override the lifecycle and
 * be invoked by LibGDX on every world switch. That really happens: a menu world with
 * a show() that restarts its intro animation had that animation cancelled the moment
 * the world was displayed, because setScreen() called it.
 *
 * Keeping the Screen implementation in a separate object means a world's own methods
 * are called only by the project's own code, exactly as in Greenfoot.
 *
 * @author Qiupi3
 * @version 1.0
 */
public class WorldScreen implements Screen {
    private final World world;

    /**
     * Wrap a world so LibGDX can display it.
     *
     * @param world the world to display; must not be null
     */
    public WorldScreen(World world) {
        if (world == null) {
            throw new IllegalArgumentException("World must not be null.");
        }
        this.world = world;
    }

    /**
     * The world this screen displays.
     *
     * @return the wrapped world
     */
    public World getWorld() {
        return world;
    }

    @Override
    public void show() {
        // Nothing to do: a Greenfoot world has no "became visible" callback.
        // started()/stopped() are driven by the simulation, not by screen switches.
    }

    @Override
    public void render(float delta) {
        world.renderFrame(delta);
    }

    @Override
    public void resize(int width, int height) {
        world.resizeViewport(width, height);
    }

    @Override
    public void pause() {
        // No world-level pause behaviour; Greenfoot.stop() controls the simulation.
    }

    @Override
    public void resume() {
        // See pause().
    }

    @Override
    public void hide() {
        // Nothing to do; the world keeps its state so it can be shown again.
    }

    @Override
    public void dispose() {
        world.disposeWorld();
    }
}
