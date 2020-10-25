package software.amazon.kms.grant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static software.amazon.kms.grant.BaseHandlerStd.THROTTLING_ERROR_CODE;


import com.amazonaws.AmazonServiceException;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DependencyTimeoutException;
import software.amazon.awssdk.services.kms.model.InvalidArnException;
import software.amazon.awssdk.services.kms.model.InvalidGrantIdException;
import software.amazon.awssdk.services.kms.model.KmsInternalException;
import software.amazon.awssdk.services.kms.model.KmsInvalidStateException;
import software.amazon.awssdk.services.kms.model.NotFoundException;
import software.amazon.awssdk.services.kms.model.RevokeGrantRequest;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandlerTest extends AbstractTestBase {
    @Mock
    private KmsClient kms;

    private DeleteHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;
    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<KmsClient> proxyKmsClient;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
        request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder()
                .grantId("grantId")
                .keyId("keyId")
                .build())
            .build();
        proxy = Mockito.spy(new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS,
            () -> Duration.ofSeconds(600).toMillis()));
        proxyKmsClient = MOCK_PROXY(proxy, kms);
    }

    @AfterEach
    public void post_execute() {
        verify(kms, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(proxyKmsClient.client());
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger);

        final ArgumentCaptor<RevokeGrantRequest> revokeGrantRequestCaptor = ArgumentCaptor
            .forClass(RevokeGrantRequest.class);
        verify(kms).revokeGrant(revokeGrantRequestCaptor.capture());
        assertThat(Translator.translateToRevokeGrantRequest(request.getDesiredResourceState())
            .equalsBySdkFields(revokeGrantRequestCaptor.getValue())).isTrue();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_GrantNotFound() {
        doThrow(NotFoundException.class).when(proxy)
            .injectCredentialsAndInvokeV2(any(RevokeGrantRequest.class), any());

        assertThrows(CfnNotFoundException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }

    @ParameterizedTest
    @ValueSource(classes = {KmsInternalException.class, DependencyTimeoutException.class})
    public void handleRequest_KmsInternalError(final Class<? extends Exception> exception) {
        doThrow(exception).when(proxy)
            .injectCredentialsAndInvokeV2(any(RevokeGrantRequest.class), any());

        assertThrows(CfnServiceInternalErrorException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }

    @ParameterizedTest
    @ValueSource(classes = {InvalidArnException.class, InvalidGrantIdException.class,
        KmsInvalidStateException.class})
    public void handleRequest_InvalidRequest(final Class<? extends Exception> exception) {
        doThrow(exception).when(proxy)
            .injectCredentialsAndInvokeV2(any(RevokeGrantRequest.class), any());

        assertThrows(CfnInvalidRequestException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }

    @Test
    public void handleRequest_ThrottlingException() {
        final AmazonServiceException throttlingException = new AmazonServiceException("");
        throttlingException.setErrorCode(THROTTLING_ERROR_CODE);
        doThrow(throttlingException).when(proxy)
            .injectCredentialsAndInvokeV2(any(RevokeGrantRequest.class), any());

        assertThrows(CfnThrottlingException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }

    @Test
    public void handleRequest_AmazonServiceException() {
        doThrow(new AmazonServiceException("")).when(proxy)
            .injectCredentialsAndInvokeV2(any(RevokeGrantRequest.class), any());

        assertThrows(CfnGeneralServiceException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }
}
