package software.amazon.kms.grant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static software.amazon.kms.grant.BaseHandlerStd.THROTTLING_ERROR_CODE;


import com.amazonaws.AmazonServiceException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DependencyTimeoutException;
import software.amazon.awssdk.services.kms.model.GrantConstraints;
import software.amazon.awssdk.services.kms.model.GrantListEntry;
import software.amazon.awssdk.services.kms.model.GrantOperation;
import software.amazon.awssdk.services.kms.model.InvalidArnException;
import software.amazon.awssdk.services.kms.model.InvalidMarkerException;
import software.amazon.awssdk.services.kms.model.KmsInternalException;
import software.amazon.awssdk.services.kms.model.KmsInvalidStateException;
import software.amazon.awssdk.services.kms.model.ListGrantsRequest;
import software.amazon.awssdk.services.kms.model.ListGrantsResponse;
import software.amazon.awssdk.services.kms.model.NotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandlerTest extends AbstractTestBase {
    protected static final ResourceModel GRANT_RESOURCE_MODEL = ResourceModel.builder()
        .keyId("keyId")
        .granteePrincipal("granteePrincipal")
        .retiringPrincipal("retiringPrincipal")
        .operations(
            Arrays.asList(GrantOperation.ENCRYPT.toString(), GrantOperation.DECRYPT.toString()))
        .constraints(Constraints.builder()
            .encryptionContextEquals(Collections.singleton(KeyValuePair.builder()
                .key("eq")
                .value("test")
                .build()))
            .encryptionContextSubset(Collections.singleton(KeyValuePair.builder()
                .key("sub")
                .value("test")
                .build()))
            .build())
        .name("name")
        .grantId("grantId")
        .build();
    private static final GrantListEntry GRANT_LIST_ENTRY = GrantListEntry.builder()
        .keyId(GRANT_RESOURCE_MODEL.getKeyId())
        .granteePrincipal(GRANT_RESOURCE_MODEL.getGranteePrincipal())
        .retiringPrincipal(GRANT_RESOURCE_MODEL.getRetiringPrincipal())
        .operations(GRANT_RESOURCE_MODEL.getOperations().stream().map(GrantOperation::fromValue)
            .collect(Collectors.toList()))
        .constraints(GrantConstraints.builder()
            .encryptionContextEquals(
                GRANT_RESOURCE_MODEL.getConstraints().getEncryptionContextEquals().stream()
                    .collect(Collectors.toMap(KeyValuePair::getKey, KeyValuePair::getValue)))
            .encryptionContextSubset(
                GRANT_RESOURCE_MODEL.getConstraints().getEncryptionContextSubset().stream()
                    .collect(Collectors.toMap(KeyValuePair::getKey, KeyValuePair::getValue)))
            .build())
        .name(GRANT_RESOURCE_MODEL.getName())
        .grantId(GRANT_RESOURCE_MODEL.getGrantId())
        .build();

    @Mock
    private KmsClient kms;

    private ReadHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;
    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<KmsClient> proxyKmsClient;

    @BeforeEach
    public void setup() {
        handler = new ReadHandler();
        request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(GRANT_RESOURCE_MODEL)
            .build();
        proxy = spy(new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS,
            () -> Duration.ofSeconds(600).toMillis()));
        proxyKmsClient = MOCK_PROXY(proxy, kms);
    }

    @AfterEach
    public void post_execute() {
        verifyNoMoreInteractions(proxyKmsClient.client());
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        List<GrantListEntry> grants = Collections.singletonList(GRANT_LIST_ENTRY);

        doReturn(ListGrantsResponse.builder()
            .grants(Collections.emptyList())
            .nextMarker("abc")
            .build())
            .doReturn(ListGrantsResponse.builder().grants(grants).build())
            .when(proxy)
            .injectCredentialsAndInvokeV2(any(ListGrantsRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(GRANT_RESOURCE_MODEL);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_GrantNotFound() {
        final ListGrantsResponse listGrantsResponse = ListGrantsResponse.builder()
            .grants(Collections.emptyList())
            .build();
        doReturn(listGrantsResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(ListGrantsRequest.class), any());

        assertThrows(CfnNotFoundException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }

    @Test
    public void handleRequest_KeyNotFound() {
        doThrow(NotFoundException.class).when(proxy)
            .injectCredentialsAndInvokeV2(any(ListGrantsRequest.class), any());

        assertThrows(CfnNotFoundException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }

    @ParameterizedTest
    @ValueSource(classes = {KmsInternalException.class, DependencyTimeoutException.class})
    public void handleRequest_KmsInternalError(final Class<? extends Exception> exception) {
        doThrow(exception).when(proxy)
            .injectCredentialsAndInvokeV2(any(ListGrantsRequest.class), any());

        assertThrows(CfnServiceInternalErrorException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }

    @ParameterizedTest
    @ValueSource(classes = {InvalidArnException.class, KmsInvalidStateException.class})
    public void handleRequest_InvalidRequest(final Class<? extends Exception> exception) {
        doThrow(exception).when(proxy)
            .injectCredentialsAndInvokeV2(any(ListGrantsRequest.class), any());

        assertThrows(CfnInvalidRequestException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }

    @Test
    public void handleRequest_InvalidMarker() {
        doThrow(InvalidMarkerException.class).when(proxy)
            .injectCredentialsAndInvokeV2(any(ListGrantsRequest.class), any());

        assertThrows(CfnInternalFailureException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }

    @Test
    public void handleRequest_ThrottlingException() {
        final AmazonServiceException throttlingException = new AmazonServiceException("");
        throttlingException.setErrorCode(THROTTLING_ERROR_CODE);
        doThrow(throttlingException).when(proxy)
            .injectCredentialsAndInvokeV2(any(ListGrantsRequest.class), any());

        assertThrows(CfnThrottlingException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }

    @Test
    public void handleRequest_AmazonServiceException() {
        doThrow(new AmazonServiceException("")).when(proxy)
            .injectCredentialsAndInvokeV2(any(ListGrantsRequest.class), any());

        assertThrows(CfnGeneralServiceException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }
}
