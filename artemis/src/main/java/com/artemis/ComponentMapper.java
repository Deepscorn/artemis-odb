package com.artemis;

import com.artemis.annotations.DelayedComponentRemoval;
import com.artemis.utils.Bag;

import static com.artemis.utils.reflect.ClassReflection.isAnnotationPresent;

/**
 * Provide high performance component access and mutation from within a System.
 *
 * This is the recommended way to mutate composition and access components.
 * Component Mappers are as fast as Transmuters.
 *
 * @param <A> Component type to map.
 * @see EntityEdit for a list of alternate ways to alter composition and access components.
 */
public class ComponentMapper<A extends Component> extends BaseComponentMapper<A> {

	/** Holds all components of given type in the world. */
	final Bag<A> components;

	private final EntityTransmuter createTransmuter;
	private final EntityTransmuter removeTransmuter;
	private final ComponentPool pool;
	private final ComponentRemover<A> purgatory;


	public ComponentMapper(Class<A> type, World world) {
		super(world.getComponentManager().typeFactory.getTypeFor(type));
		components = new Bag<A>(type);

		pool = (this.type.isPooled)
			? new ComponentPool(type)
			: null;

		if (isAnnotationPresent(type, DelayedComponentRemoval.class))
			purgatory = new DelayedComponentRemover<A>(components, pool, world.batchProcessor);
		else
			purgatory = new ImmediateComponentRemover<A>(components, pool);

		createTransmuter = new EntityTransmuterFactory(world).add(type).build();
		removeTransmuter = new EntityTransmuterFactory(world).remove(type).build();
	}

	/**
	 * Fast retrieval of a component for this entity.
	 *
	 * Safe as long as the entity exists.
	 *
	 * @param entityId the entity id that should possess the component. Must be in scope of world.
	 * @return the instance of the component, or {@code null} if none.
	 * @throws ArrayIndexOutOfBoundsException when entity is out of scope of world.
	 * @see #getSafe(int, Component)
	 */
	@Override
	public A get(int entityId) throws ArrayIndexOutOfBoundsException {
		return components.get(entityId);
	}

	/**
	 * Checks if the entity has this type of component.
	 *
	 * @param entityId the id of entity to check
	 * @return true if the entity has this component type, false if it doesn't
	 * @throws ArrayIndexOutOfBoundsException when entity is out of scope of world.
	 */
	@Override
	public boolean has(int entityId) {
		return get(entityId) != null && !purgatory.has(entityId);
	}


	/**
	 * Remove component from entity.
	 * Does nothing if already removed.
	 *
	 * @param entityId
	 */
	@Override
	public void remove(int entityId) {
		A component = get(entityId);
		if (component != null) {
			removeTransmuter.transmuteNoOperation(entityId);
			purgatory.mark(entityId);
		}
	}

	@Override
	protected void internalRemove(int entityId) { // triggers no composition id update
		A component = get(entityId);
		if (component != null)
			purgatory.mark(entityId);
	}

	/**
	 * Create component for this entity.
	 * Avoids creation if component exists.
	 *
	 * @param entityId the entity that should possess the component
	 * @return the instance of the component.
	 */
	@Override
	public A create(int entityId) {
		A component = get(entityId);
		if (component == null || purgatory.unmark(entityId)) {
			// running transmuter first, as it performs som validation
			createTransmuter.transmuteNoOperation(entityId);
			component = createNew();
			components.unsafeSet(entityId, component);
		}

		return component;
	}

	@Override
	public A internalCreate(int entityId) {
		A component = get(entityId);
		if (component == null || purgatory.unmark(entityId)) {
			component = createNew();
			components.unsafeSet(entityId, component);
		}

		return component;
	}

	private A createNew() {
		return (A) ((pool != null)
			? pool.obtain()
			: ComponentManager.newInstance(type.getType()));
	}

}
