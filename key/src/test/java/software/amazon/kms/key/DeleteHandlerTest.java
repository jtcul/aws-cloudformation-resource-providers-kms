package software.amazon.kms.key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DescribeKeyRequest;
import software.amazon.awssdk.services.kms.model.DescribeKeyResponse;
import software.amazon.awssdk.services.kms.model.KeyMetadata;
import software.amazon.awssdk.services.kms.model.KeyState;
import software.amazon.awssdk.services.kms.model.KmsInvalidStateException;
import software.amazon.awssdk.services.kms.model.ScheduleKeyDeletionRequest;
import software.amazon.awssdk.services.kms.model.ScheduleKeyDeletionResponse;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    KmsClient kms;

    @Mock
    private KeyHelper keyHelper;

    private DeleteHandler handler;
    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<KmsClient> proxyKmsClient;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler(keyHelper);
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS,
            () -> Duration.ofSeconds(600).toMillis());
        proxyKmsClient = MOCK_PROXY(proxy, kms);
    }

    @AfterEach
    public void post_execute() {
        verify(kms, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(proxyKmsClient.client());
        verifyNoMoreInteractions(keyHelper);
    }

    @Test
    public void handleRequest_PartiallyPropagate() {
        final ScheduleKeyDeletionResponse scheduleKeyDeletionResponse =
            ScheduleKeyDeletionResponse.builder().build();
        when(keyHelper
            .scheduleKeyDeletion(any(ScheduleKeyDeletionRequest.class), eq(proxyKmsClient)))
            .thenReturn(scheduleKeyDeletionResponse);

        final DescribeKeyResponse describeKeyResponse = DescribeKeyResponse.builder()
            .keyMetadata(KeyMetadata.builder().keyState(KeyState.PENDING_DELETION).build())
            .build();
        when(keyHelper.describeKey(any(DescribeKeyRequest.class), eq(proxyKmsClient)))
            .thenReturn(describeKeyResponse);

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().build())
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request,
                new CallbackContext(), proxyKmsClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(60);
        assertThat(response.getCallbackContext().isPropagated()).isTrue();
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(keyHelper)
            .scheduleKeyDeletion(any(ScheduleKeyDeletionRequest.class), eq(proxyKmsClient));
        verify(keyHelper).describeKey(any(DescribeKeyRequest.class), eq(proxyKmsClient));
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ScheduleKeyDeletionResponse scheduleKeyDeletionResponse =
                ScheduleKeyDeletionResponse.builder().build();
        when(keyHelper
                .scheduleKeyDeletion(any(ScheduleKeyDeletionRequest.class), eq(proxyKmsClient)))
                .thenReturn(scheduleKeyDeletionResponse);

        final DescribeKeyResponse describeKeyResponse = DescribeKeyResponse.builder()
                .keyMetadata(KeyMetadata.builder().keyState(KeyState.PENDING_DELETION).build())
                .build();
        when(keyHelper.describeKey(any(DescribeKeyRequest.class), eq(proxyKmsClient)))
                .thenReturn(describeKeyResponse);

        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(ResourceModel.builder().build())
                        .build();

        final CallbackContext callbackContext = new CallbackContext();
        callbackContext.setPropagated(true);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                callbackContext, proxyKmsClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(keyHelper)
                .scheduleKeyDeletion(any(ScheduleKeyDeletionRequest.class), eq(proxyKmsClient));
        verify(keyHelper).describeKey(any(DescribeKeyRequest.class), eq(proxyKmsClient));
    }

    // Key has been scheduled for deletion out of band -> considered deleted
    @Test
    public void handleRequest_InvalidState() {
        when(keyHelper
            .scheduleKeyDeletion(any(ScheduleKeyDeletionRequest.class), eq(proxyKmsClient)))
            .thenThrow(new CfnInvalidRequestException(KmsInvalidStateException.builder().build()));

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().build())
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request,
                new CallbackContext(), proxyKmsClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

        verify(keyHelper)
            .scheduleKeyDeletion(any(ScheduleKeyDeletionRequest.class), eq(proxyKmsClient));
    }
}
