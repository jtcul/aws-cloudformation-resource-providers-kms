package software.amazon.kms.grant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.kms.grant.BaseHandlerStd.THROTTLING_ERROR_CODE;


import com.amazonaws.AmazonServiceException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.CreateGrantRequest;
import software.amazon.awssdk.services.kms.model.CreateGrantResponse;
import software.amazon.awssdk.services.kms.model.DependencyTimeoutException;
import software.amazon.awssdk.services.kms.model.DisabledException;
import software.amazon.awssdk.services.kms.model.GrantOperation;
import software.amazon.awssdk.services.kms.model.InvalidArnException;
import software.amazon.awssdk.services.kms.model.InvalidGrantTokenException;
import software.amazon.awssdk.services.kms.model.KmsInternalException;
import software.amazon.awssdk.services.kms.model.KmsInvalidStateException;
import software.amazon.awssdk.services.kms.model.LimitExceededException;
import software.amazon.awssdk.services.kms.model.NotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandlerTest extends AbstractTestBase {
    private static final ResourceModel.ResourceModelBuilder GRANT_RESOURCE_MODEL_BUILDER =
        ResourceModel.builder()
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
            .grantTokens(Collections.singletonList("token"))
            .name("name");
    private static final ResourceModel GRANT_RESOURCE_MODEL =
        GRANT_RESOURCE_MODEL_BUILDER.build();
    private static final ResourceModel GRANT_RESOURCE_MODEL_CREATED = GRANT_RESOURCE_MODEL_BUILDER
        .grantId("grantId")
        .build();

    @Mock
    private KmsClient kms;

    private CreateHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;
    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<KmsClient> proxyKmsClient;

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();
        request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(GRANT_RESOURCE_MODEL)
            .build();
        proxy = Mockito
            .spy(new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS,
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
        when(kms.createGrant(any(CreateGrantRequest.class)))
            .thenReturn(CreateGrantResponse.builder()
                .grantId(GRANT_RESOURCE_MODEL_CREATED.getGrantId())
                .build());

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger);

        final ArgumentCaptor<CreateGrantRequest> createGrantRequestCaptor = ArgumentCaptor
            .forClass(CreateGrantRequest.class);
        verify(kms).createGrant(createGrantRequestCaptor.capture());
        assertThat(Translator.translateToCreateGrantRequest(request.getDesiredResourceState())
            .equalsBySdkFields(createGrantRequestCaptor.getValue())).isTrue();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(GRANT_RESOURCE_MODEL_CREATED);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_KeyNotFound() {
        doThrow(NotFoundException.class).when(proxy)
            .injectCredentialsAndInvokeV2(any(CreateGrantRequest.class), any());

        assertThrows(CfnNotFoundException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }

    @ParameterizedTest
    @ValueSource(classes = {KmsInternalException.class, DependencyTimeoutException.class})
    public void handleRequest_KmsInternalError(final Class<? extends Exception> exception) {
        doThrow(exception).when(proxy)
            .injectCredentialsAndInvokeV2(any(CreateGrantRequest.class), any());

        assertThrows(CfnServiceInternalErrorException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }

    @ParameterizedTest
    @ValueSource(classes = {InvalidArnException.class, InvalidGrantTokenException.class,
        KmsInvalidStateException.class, DisabledException.class})
    public void handleRequest_InvalidRequest(final Class<? extends Exception> exception) {
        doThrow(exception).when(proxy)
            .injectCredentialsAndInvokeV2(any(CreateGrantRequest.class), any());

        assertThrows(CfnInvalidRequestException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }

    @Test
    public void handleRequest_LimitExceededException() {
        doThrow(LimitExceededException.class).when(proxy)
            .injectCredentialsAndInvokeV2(any(CreateGrantRequest.class), any());

        assertThrows(CfnServiceLimitExceededException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }

    @Test
    public void handleRequest_ThrottlingException() {
        final AmazonServiceException throttlingException = new AmazonServiceException("");
        throttlingException.setErrorCode(THROTTLING_ERROR_CODE);
        doThrow(throttlingException).when(proxy)
            .injectCredentialsAndInvokeV2(any(CreateGrantRequest.class), any());

        assertThrows(CfnThrottlingException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }

    @Test
    public void handleRequest_AmazonServiceException() {
        doThrow(new AmazonServiceException("")).when(proxy)
            .injectCredentialsAndInvokeV2(any(CreateGrantRequest.class), any());

        assertThrows(CfnGeneralServiceException.class, () -> handler
            .handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger));
    }
}
