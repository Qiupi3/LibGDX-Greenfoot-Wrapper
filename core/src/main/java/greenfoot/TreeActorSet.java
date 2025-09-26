package greenfoot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TreeActorSet is a specialized collection for managing Actor objects with class-based ordering.
 * This is typically used in Greenfoot worlds to maintain paint order and processing order
 * of actors based on their class hierarchy.
 * 
 * Compatible with LibGDX as it uses standard Java collections.
 */
public class TreeActorSet extends ArrayList<Actor> {
    private List<Class<?>> classOrder = new ArrayList<>();
    private boolean paintOrder = true;

    /**
     * Set the class order for actors in this set.
     * 
     * @param paintOrder Whether this ordering affects paint order
     * @param classes The classes in order (first class gets highest priority)
     */
    public void setClassOrder(boolean paintOrder, Class<?>... classes) {
        this.paintOrder = paintOrder;
        classOrder.clear();
        if (classes != null) {
            classOrder.addAll(Arrays.asList(classes));
        }
        sortByClassOrder();
    }
    
    /**
     * Add an actor and maintain class ordering.
     * 
     * @param actor The actor to add
     * @return true if the actor was added
     */
    @Override
    public boolean add(Actor actor) {
        boolean result = super.add(actor);
        if (result && !classOrder.isEmpty()) {
            sortByClassOrder();
        }
        return result;
    }

    /**
     * Sort actors by their class order as specified in setClassOrder.
     */
    private void sortByClassOrder() {
        if (classOrder.isEmpty()) return;

        this.sort((a, b) -> {
            int aIndex = getClassIndex(a.getClass());
            int bIndex = getClassIndex(b.getClass());
            return Integer.compare(aIndex, bIndex);
        });
    }

    /**
     * Get the index of a class in the class order.
     * 
     * @param cls The class to find
     * @return The index, or size of classOrder if not found
     */
    private int getClassIndex(Class<?> cls) {
        for (int i = 0; i < classOrder.size(); i++) {
            if (classOrder.get(i).isAssignableFrom(cls)) {
                return i;
            }
        }
        return classOrder.size(); // Unknown classes go to the end
    }
    
    /**
     * Get whether this set uses paint order.
     * 
     * @return true if paint order is used
     */
    public boolean isPaintOrder() {
        return paintOrder;
    }
}
