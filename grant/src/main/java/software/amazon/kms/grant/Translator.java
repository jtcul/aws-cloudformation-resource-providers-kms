package software.amazon.kms.grant;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import software.amazon.awssdk.services.kms.model.CreateGrantRequest;
import software.amazon.awssdk.services.kms.model.GrantConstraints;
import software.amazon.awssdk.services.kms.model.GrantListEntry;
import software.amazon.awssdk.services.kms.model.GrantOperation;
import software.amazon.awssdk.services.kms.model.ListGrantsRequest;
import software.amazon.awssdk.services.kms.model.RevokeGrantRequest;

public class Translator {
    static CreateGrantRequest translateToCreateGrantRequest(
        @Nonnull final ResourceModel resourceModel) {
        return CreateGrantRequest.builder()
            .keyId(resourceModel.getKeyId())
            .granteePrincipal(resourceModel.getGranteePrincipal())
            .retiringPrincipal(resourceModel.getRetiringPrincipal())
            .operations(Optional.ofNullable(resourceModel.getOperations())
                .map(Translator::translateOperationsToSdk)
                .orElse(null))
            .constraints(Optional.ofNullable(resourceModel.getConstraints())
                .map(Translator::translateConstraintsToSdk)
                .orElse(null))
            .grantTokens(resourceModel.getGrantTokens())
            .name(resourceModel.getName())
            .build();
    }

    static RevokeGrantRequest translateToRevokeGrantRequest(
        @Nonnull final ResourceModel resourceModel) {
        return RevokeGrantRequest.builder()
            .grantId(resourceModel.getGrantId())
            .keyId(resourceModel.getKeyId())
            .build();
    }

    static ListGrantsRequest translateToListGrantsRequest(
        @Nonnull final ResourceModel resourceModel,
        @Nullable final String marker) {
        return ListGrantsRequest.builder()
            .marker(marker)
            .keyId(resourceModel.getKeyId())
            .build();
    }

    static Collection<GrantOperation> translateOperationsToSdk(
        @Nonnull final Collection<String> operations) {
        return operations.stream().map(GrantOperation::fromValue).collect(Collectors.toList());
    }

    static Map<String, String> translateKeyValuePairsToSdk(
        @Nonnull final Set<KeyValuePair> keyValuePairs) {
        return keyValuePairs.stream()
            .collect(Collectors.toMap(KeyValuePair::getKey, KeyValuePair::getValue));
    }

    static Set<KeyValuePair> translateKeyValuePairsFromSdk(
        @Nonnull final Map<String, String> keyValuePairs) {
        return keyValuePairs.entrySet().stream().map(e -> KeyValuePair.builder()
            .key(e.getKey())
            .value(e.getValue())
            .build())
            .collect(Collectors.toSet());
    }

    static GrantConstraints translateConstraintsToSdk(@Nonnull final Constraints constraints) {
        return GrantConstraints.builder()
            .encryptionContextSubset(Optional.ofNullable(constraints.getEncryptionContextSubset())
                .map(Translator::translateKeyValuePairsToSdk)
                .orElse(null))
            .encryptionContextEquals(Optional.ofNullable(constraints.getEncryptionContextEquals())
                .map(Translator::translateKeyValuePairsToSdk)
                .orElse(null))
            .build();
    }

    static Constraints translateConstraintsFromSdk(
        @Nonnull final GrantConstraints grantConstraints) {
        return Constraints.builder()
            .encryptionContextSubset(Optional.ofNullable(grantConstraints.encryptionContextSubset())
                .map(Translator::translateKeyValuePairsFromSdk)
                .orElse(null))
            .encryptionContextEquals(Optional.ofNullable(grantConstraints.encryptionContextEquals())
                .map(Translator::translateKeyValuePairsFromSdk)
                .orElse(null))
            .build();
    }

    static ResourceModel translateToResourceModel(@Nonnull final GrantListEntry grantListEntry) {
        return ResourceModel.builder()
            .keyId(grantListEntry.keyId())
            .granteePrincipal(grantListEntry.granteePrincipal())
            .retiringPrincipal(grantListEntry.retiringPrincipal())
            .operations(grantListEntry.operationsAsStrings())
            .constraints(Optional.ofNullable(grantListEntry.constraints())
                .map(Translator::translateConstraintsFromSdk)
                .orElse(null))
            .name(grantListEntry.name())
            .grantId(grantListEntry.grantId())
            .build();
    }
}
