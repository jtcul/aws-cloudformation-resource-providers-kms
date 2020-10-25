package software.amazon.kms.grant;

import com.amazonaws.AmazonServiceException;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.CreateGrantRequest;
import software.amazon.awssdk.services.kms.model.CreateGrantResponse;
import software.amazon.awssdk.services.kms.model.DependencyTimeoutException;
import software.amazon.awssdk.services.kms.model.DisabledException;
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
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends BaseHandlerStd {
    private static final String OPERATION = "CreateGrant";

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<KmsClient> proxyClient,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        return proxy.initiate("kms::create-grant", proxyClient, model, callbackContext)
            .translateToServiceRequest(Translator::translateToCreateGrantRequest)
            .makeServiceCall(this::createGrant)
            .done(createGrantResponse -> {
                model.setGrantId(createGrantResponse.grantId());

                return ProgressEvent.defaultSuccessHandler(model);
            });
    }

    private CreateGrantResponse createGrant(final CreateGrantRequest createGrantRequest,
                                            final ProxyClient<KmsClient> proxyClient) {
        try {
            return proxyClient.injectCredentialsAndInvokeV2(createGrantRequest,
                proxyClient.client()::createGrant);
        } catch (final NotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (final KmsInternalException | DependencyTimeoutException e) {
            throw new CfnServiceInternalErrorException(OPERATION, e);
        } catch (final InvalidArnException | InvalidGrantTokenException | KmsInvalidStateException
            | DisabledException e) {
            throw new CfnInvalidRequestException(e.getMessage());
        } catch (final LimitExceededException e) {
            throw new CfnServiceLimitExceededException(e);
        } catch (final AmazonServiceException exception) {
            if (THROTTLING_ERROR_CODE.equals(exception.getErrorCode())) {
                throw new CfnThrottlingException(OPERATION, exception);
            }

            throw new CfnGeneralServiceException(OPERATION, exception);
        }
    }
}
