package org.tavall.dependency.access.fixtures;

import org.tavall.dependency.DependencyAccess;
import org.tavall.dependency.IDependencyAccess;
import org.tavall.dependency.annotations.DelegatesTo;

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

@DelegatesTo
final class FiveDependencyAccess implements IDependencyAccess {
    public IPlayerRegistry playerRegistry() {
        return getDependencyMap().getInstance(IPlayerRegistry.class);
    }

    public IPlayerDataRepository playerDataRepository() {
        return getDependencyMap().getInstance(IPlayerDataRepository.class);
    }

    public IWalletRegistry walletRegistry() {
        return getDependencyMap().getInstance(IWalletRegistry.class);
    }

    public ITransactionRepository transactionRepository() {
        return getDependencyMap().getInstance(ITransactionRepository.class);
    }

    public IMessageFormatter messageFormatter() {
        return getDependencyMap().getInstance(IMessageFormatter.class);
    }
}

@DelegatesTo(IRewardHandler.class)
public final class FiveDependencyHandler<HandlerValue>
        implements IRewardHandler, DependencyAccess<FiveDependencyAccess> {
}
