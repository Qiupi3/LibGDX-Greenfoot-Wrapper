package greenfoot;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import java.util.AbstractSet;
import java.util.Iterator;

/**
 * LibGDX-based ActorSet implementation.
 */
public class ActorSet extends AbstractSet<Actor> {
    
    private ObjectSet<Actor> actorSet;
    private Array<Actor> actorList;
    private int totalHashCode = 0;
    
    public ActorSet() {
        actorSet = new ObjectSet<Actor>();
        actorList = new Array<Actor>();
    }
    
    @Override
    public int hashCode() {
        return totalHashCode;
    }
    
    @Override
    public boolean add(Actor actor) {
        if (actor == null || actorSet.contains(actor)) {
            return false;
        }
        actorSet.add(actor);
        actorList.add(actor);
        totalHashCode += actor.getSequenceNumber();
        return true;
    }
    
    public boolean containsActor(Actor actor) {
        return actor != null && actorSet.contains(actor);
    }
    
    @Override
    public boolean contains(Object o) {
        return o instanceof Actor && containsActor((Actor) o);
    }
    
    public boolean remove(Actor actor) {
        if (actor == null || !actorSet.contains(actor)) {
            return false;
        }
        actorSet.remove(actor);
        actorList.removeValue(actor, true);
        totalHashCode -= actor.getSequenceNumber();
        return true;
    }
    
    @Override
    public boolean remove(Object o) {
        return o instanceof Actor && remove((Actor) o);
    }
    
    @Override
    public int size() {
        return actorSet.size;
    }
    
    @Override
    public Iterator<Actor> iterator() {
        return new ActorSetIterator();
    }
    
    private class ActorSetIterator implements Iterator<Actor> {
        private int currentIndex = 0;
        private Actor lastReturned = null;
        
        @Override
        public boolean hasNext() {
            return currentIndex < actorList.size;
        }
        
        @Override
        public Actor next() {
            if (!hasNext()) {
                throw new java.util.NoSuchElementException();
            }
            lastReturned = actorList.get(currentIndex);
            currentIndex++;
            return lastReturned;
        }
        
        @Override
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            ActorSet.this.remove(lastReturned);
            currentIndex--;
            lastReturned = null;
        }
    }
}
