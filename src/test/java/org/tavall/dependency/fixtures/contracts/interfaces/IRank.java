package org.tavall.dependency.fixtures.contracts.interfaces;

import java.util.Collections;
import java.util.List;

public interface IRank extends org.tavall.dependency.IDependencyInjectableInterface {

    default List<String> getRanks() {
        return Collections.emptyList();
    }
}
