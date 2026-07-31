package org.tavall.dependency.access.fixtures;

import org.tavall.dependency.DependencyAccess;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.dependency.annotations.GrantDependencyAccess;
import org.tavall.dependency.annotations.GrantedDependencyAccess;
import org.tavall.dependency.annotations.VariableTypeArguments;

@VariableTypeArguments
interface PlayerAccess<FirstDependency, SecondDependency, ThirdDependency, FourthDependency, FifthDependency> extends DependencyAccess {
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
@DelegatesTo(IRewardHandler.class)
public final class FiveDependencyHandler<HandlerValue>
        implements PlayerAccess<IPlayerRegistry, IPlayerDataRepository, IWalletRegistry, ITransactionRepository, IMessageFormatter> {
}
