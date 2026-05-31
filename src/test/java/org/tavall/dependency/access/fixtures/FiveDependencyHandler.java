package org.tavall.dependency.access.fixtures;

import org.tavall.dependency.annotations.DepAccess;
import org.tavall.dependency.annotations.DelegatesToInterface;
import org.tavall.dependency.annotations.GrantDependencyAccess;
import org.tavall.dependency.annotations.GrantedDependencyAccess;
import org.tavall.dependency.annotations.VariableTypeArguments;

@VariableTypeArguments
interface PlayerAccess<FirstDependency, SecondDependency, ThirdDependency, FourthDependency, FifthDependency> extends DepAccess {
}

interface IPlayerRegistry {
}

interface IPlayerDataRepository {
}

interface IWalletRegistry {
}

interface ITransactionRepository {
}

interface IMessageFormatter {
}

interface IRewardHandler {
}

@GrantDependencyAccess
@GrantedDependencyAccess(
        accessType = PlayerAccess.class,
        dependencyTypes = {
                IPlayerRegistry.class,
                IPlayerDataRepository.class,
                IWalletRegistry.class,
                ITransactionRepository.class,
                IMessageFormatter.class
        }
)
@DelegatesToInterface(value = IRewardHandler.class)
public final class FiveDependencyHandler<HandlerValue>
        implements PlayerAccess<IPlayerRegistry, IPlayerDataRepository, IWalletRegistry, ITransactionRepository, IMessageFormatter> {
}
