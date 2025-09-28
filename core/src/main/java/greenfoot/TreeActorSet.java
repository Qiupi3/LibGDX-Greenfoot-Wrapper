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

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * TreeActorSet is a specialized collection for managing Actor objects with class-based ordering.
 * This is typically used in Greenfoot worlds to maintain paint order and processing order
 * of actors based on their class hierarchy.
 * 
 * Compatible with LibGDX as it uses standard Java collections and AbstractSet.
 * 
 * This class re-implements greenfoot.TreeActorSet to provide a LibGDX backend,
 * mainly to allow Greenfoot projects to run on LibGDX (especially to export into
 * mobile devices and other platforms).
 * 
 * Inspired by the original Greenfoot project (GPLv2+ with Classpath Exception).
 * Read the original documentation at
 * https://www.greenfoot.org/files/javadoc/greenfoot/package-summary.html
 * 
 * @author Davin McCall (Original Greenfoot version's author)
 * 
 * @modified-by Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */
public class TreeActorSet extends AbstractSet<Actor> {
    
    private LinkedList<ActorSet> subSets;
    private ActorSet generalSet;
    private HashMap<Class<?>, ActorSet> classSets;
    
    /**
     * Constructs a new TreeActorSet with default ordering.
     */
    public TreeActorSet() {
        subSets = new LinkedList<ActorSet>();
        generalSet = new ActorSet();
        subSets.add(generalSet);
        
        classSets = new HashMap<Class<?>, ActorSet>();
    }
    
    /**
     * Set the iteration order of objects. The first given class will have
     * objects of its class last in the iteration order, the next will have
     * objects second last in iteration order, and so on. This holds if it is
     * reversed. If it is not reversed it will return the first one first and so
     * on.
     * 
     * Objects not belonging to any of the specified classes will be first in
     * the iteration order if reversed, or last if not reversed
     * 
     * @param reverse Whether to reverse the order or not. 
     */
    public void setClassOrder(boolean reverse, Class<?> ... classes) {
        HashMap<Class<?>, ActorSet> oldClassSets = classSets;
        classSets = new HashMap<Class<?>, ActorSet>();
        
        // A list of classes we need to sweep the superclass set of
        LinkedList<Class<?>> sweepClasses = new LinkedList<Class<?>>();
        
        // For each listed class, use the ActorSet from the original classSets
        // if it exists, or create a new one if not
        for (int i = 0; i < classes.length; i++) {
            ActorSet oldSet = oldClassSets.remove(classes[i]);
            if (oldSet == null) {
                // There was no old set for this class. We'll need to check
                // the superclass set for actors which actually belong in
                // the new set.
                sweepClasses.add(classes[i]);
                oldSet = new ActorSet();
            }
            classSets.put(classes[i], oldSet);
        }
        
        // There may be objects in a set for some class A which
        // belong in the set for class B which is derived from A.
        // Now we'll "sweep" such sets.

        Set<Class<?>> sweptClasses = new HashSet<Class<?>>();
        
        while (! sweepClasses.isEmpty()) {
            Class<?> sweepClass = sweepClasses.removeFirst();
            if (sweepClass != null) {
                sweepClass = sweepClass.getSuperclass();
            }
            ActorSet sweepSet = null;
            if (sweepClass != null) {
                sweepSet = classSets.get(sweepClass);
            }
            while (sweepSet == null) {
                if (sweepClass == null) {
                    sweepSet = generalSet;
                    break;
                }
                sweepClass = sweepClass.getSuperclass();
                if (sweepClass != null) {
                    sweepSet = classSets.get(sweepClass);
                }
            }
            
            if (sweepClass != null && ! sweptClasses.contains(sweepClass)) {
                sweptClasses.add(sweepClass);
                // go through sweep set
                Iterator<Actor> i = sweepSet.iterator();
                while (i.hasNext()) {
                    Actor actor = i.next();
                    ActorSet set = setForActor(actor);
                    if (set != sweepSet) {
                        set.add(actor); // add to the specific set
                        i.remove(); // remove from the general set
                    }
                }
            }
        }
        
        // Now, for any old subsets not yet handled, move all the actors into
        // the appropriate set. ("Not yet handled" means that the old subset
        // has no equivalent in the new sets).
        Iterator<Map.Entry<Class<?>,ActorSet>> ei = oldClassSets.entrySet().iterator();
        for ( ; ei.hasNext(); ) {
            Map.Entry<Class<?>,ActorSet> entry = ei.next();
            ActorSet destinationSet = setForClass(entry.getKey());
            destinationSet.addAll(entry.getValue());
        }
        
        // Finally, re-create the subsets list
        subSets.clear();
        if(reverse) {
            subSets.add(generalSet);
            for (int i = classes.length; i > 0; ) {
                subSets.add(classSets.get(classes[--i]));
            }
        }
        else {
            for (int i = 0; i < classes.length; i++) {
                subSets.add(classSets.get(classes[i]));
            }
            subSets.add(generalSet);
        }
    }
    
    /**
     * Returns an iterator over the actors in this set.
     */
    public Iterator<Actor> iterator() {
        return new TasIterator();
    }

    /**
     * Returns the number of actors in this set.
     */
    @Override
    public int size() {
        int size = 0;
        for (Iterator<ActorSet> i = subSets.iterator(); i.hasNext(); ) {
            size += i.next().size();
        }
        return size;
    }

    /**
     * Adds an actor to this set.
     */
    @Override
    public boolean add(Actor o) {
        if (o == null) {
            throw new UnsupportedOperationException("Cannot add null actor.");
        }
        
        return setForActor(o).add(o);
    }
    
    /**
     * Removes an actor from this set.
     */
    public boolean remove(Actor o) {
        return setForActor(o).remove(o);
    }

    /**
     * Checks if this set contains the specified actor.
     */
    public boolean contains(Actor o) {
        return setForActor(o).containsActor(o);
    }
    
    /**
     * Get the appropriate ActorSet for the given actor based on its class.
     */
    private ActorSet setForActor(Actor actor) {
        return setForClass(actor.getClass());
    }
    
    /**
     * Get the appropriate ActorSet for the given class.
     */
    private ActorSet setForClass(Class<?> cls) {
        // Look for a specific set for this class or its superclasses
        Class<?> currentClass = cls;
        while (currentClass != null) {
            ActorSet set = classSets.get(currentClass);
            if (set != null) {
                return set;
            }
            currentClass = currentClass.getSuperclass();
        }
        
        // No specific set found, use the general set
        return generalSet;
    }
    
    /**
     * Iterator implementation for TreeActorSet.
     */
    private class TasIterator implements Iterator<Actor> {
        private Iterator<ActorSet> subSetIterator;
        private Iterator<Actor> currentActorIterator;
        
        public TasIterator() {
            subSetIterator = subSets.iterator();
            if (subSetIterator.hasNext()) {
                currentActorIterator = subSetIterator.next().iterator();
            }
        }
        
        @Override
        public boolean hasNext() {
            while (currentActorIterator != null && !currentActorIterator.hasNext()) {
                if (subSetIterator.hasNext()) {
                    currentActorIterator = subSetIterator.next().iterator();
                } else {
                    currentActorIterator = null;
                }
            }
            
            return currentActorIterator != null && currentActorIterator.hasNext();
        }
        
        @Override
        public Actor next() {
            if (!hasNext()) {
                throw new java.util.NoSuchElementException();
            }
            
            return currentActorIterator.next();
        }
        
        @Override
        public void remove() {
            if (currentActorIterator == null) {
                throw new IllegalStateException();
            }
            
            currentActorIterator.remove();
        }
    }
}
