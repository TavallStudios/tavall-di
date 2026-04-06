package com.tjxjnoobie.api.interfaces;

import com.tjxjnoobie.api.dependency.annotations.ComposesToInterface;
import com.tjxjnoobie.api.dependency.composition.domains.IInfrastructureDomain;

import java.util.Collections;
import java.util.List;

@ComposesToInterface(IInfrastructureDomain.class)
public interface IRank extends com.tjxjnoobie.api.dependency.IDependencyInjectableInterface {

    default List<String> getRanks() {
        return Collections.emptyList();
    }
}
