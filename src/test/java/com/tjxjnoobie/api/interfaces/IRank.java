package com.tjxjnoobie.api.interfaces;

import java.util.Collections;
import java.util.List;

public interface IRank extends com.tjxjnoobie.api.dependency.IDependencyInjectableInterface {

    default List<String> getRanks() {
        return Collections.emptyList();
    }
}
