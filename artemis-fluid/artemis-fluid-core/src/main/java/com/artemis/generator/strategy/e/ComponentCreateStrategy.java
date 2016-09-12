package com.artemis.generator.strategy.e;

import com.artemis.generator.model.FluidTypes;
import com.artemis.generator.common.IterativeModelStrategy;
import com.artemis.generator.model.type.TypeModel;
import com.artemis.generator.model.artemis.ComponentDescriptor;
import com.artemis.generator.model.type.MethodDescriptor;
import com.artemis.generator.util.MethodBuilder;

/**
 * Generates create method for each component.
 *
 * @author Daan van Yperen
 */
public class ComponentCreateStrategy extends IterativeModelStrategy {

    @Override
    protected void apply(ComponentDescriptor component, TypeModel model) {
        model.add(createComponentMethod(component));
    }

    /**
     * T componentName() -> create new entity.
     */
    private MethodDescriptor createComponentMethod(ComponentDescriptor component) {
        return
                new MethodBuilder(FluidTypes.E_TYPE, component.getMethodPrefix())
                        .debugNotes(component.getComponentType().getName())
                        .mapper(component, ".create(entityId)")
                        .returnFluid()
                        .build();
    }
}