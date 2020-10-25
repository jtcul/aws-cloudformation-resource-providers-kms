package software.amazon.kms.grant;

import com.amazonaws.AmazonServiceException;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DependencyTimeoutException;
import software.amazon.awssdk.services.kms.model.InvalidArnException;
import software.amazon.awssdk.services.kms.model.InvalidGrantIdException;
import software.amazon.awssdk.services.kms.model.KmsInternalException;
import software.amazon.awssdk.services.kms.model.KmsInvalidStateException;
import software.amazon.awssdk.services.kms.model.NotFoundException;
import software.amazon.awssdk.services.kms.model.RevokeGrantRequest;
import software.amazon.awssdk.services.kms.model.RevokeGrantResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private static final String OPERATION = "RevokeGrant";

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<KmsClient> proxyClient,
        final Logger logger) {
        final ResourceModel model = request.getDesiredResourceState();

        return proxy.initiate("kms::revoke-grant", proxyClient, model, callbackContext)
            .translateToServiceRequest(m -> Translator.translateToRevokeGrantRequest(model))
            .makeServiceCall(this::revokeGrant)
            .done(awsResponse -> ProgressEvent.defaultSuccessHandler(model));
    }

    private RevokeGrantResponse revokeGrant(final RevokeGrantRequest revokeGrantRequest,
                                            final ProxyClient<KmsClient> proxyClient) {
        try {
            return proxyClient.injectCredentialsAndInvokeV2(revokeGrantRequest,
                proxyClient.client()::revokeGrant);
        } catch (final NotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, revokeGrantRequest.grantId());
        } catch (final KmsInternalException | DependencyTimeoutException e) {
            throw new CfnServiceInternalErrorException(OPERATION, e);
        } catch (final InvalidArnException | InvalidGrantIdException | KmsInvalidStateException e) {
            throw new CfnInvalidRequestException(e.getMessage());
        } catch (final AmazonServiceException exception) {
            if (THROTTLING_ERROR_CODE.equals(exception.getErrorCode())) {
                throw new CfnThrottlingException(OPERATION, exception);
            }

            throw new CfnGeneralServiceException(OPERATION, exception);
        }
    }
}
