package global.genesis.file.storage.provider

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import global.genesis.config.system.SystemDefinitionService
import kotlin.jvm.optionals.getOrElse

object AwsS3ClientBuilder {

    fun build(storageMode: String, definitionService: SystemDefinitionService): AmazonS3 {
        return when (storageMode) {
            "LOCAL" -> localAws(definitionService)
            "DEV" -> devAws(definitionService)
            "AWS" -> aws()
            else -> throw Exception("Unable to build aws s3 client because no valid storage mode has been set.")
        }
    }

    // for running app locally and using local aws bucket
    private fun localAws(definitionService: SystemDefinitionService): AmazonS3 {
        val awsHost = definitionService.get("AWS_HOST").getOrElse { "" }
        val awsRegion = definitionService.get("AWS_REGION").getOrElse { "" }

        require(awsHost.isNotBlank()) { "AWS_HOST must be set." }
        require(awsRegion.isNotBlank()) { "AWS_REGION must be set." }

        return AmazonS3ClientBuilder
            .standard()
            .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(awsHost, awsRegion))
            .build()
    }

    // for running app locally or on a different web service provider other than aws and using remote aws bucket
    private fun devAws(definitionService: SystemDefinitionService): AmazonS3 {
        val awsRegion = definitionService.get("AWS_REGION").getOrElse { "" }
        val awsAccessKey = definitionService.get("AWS_ACCESS_KEY").getOrElse { "" }
        val awsSecretAccessKey = definitionService.get("AWS_SECRET_ACCESS_KEY").getOrElse { "" }

        require(awsRegion.isNotBlank()) { "AWS_REGION must be set." }
        require(awsAccessKey.isNotBlank()) { "AWS_ACCESS_KEY must be set." }
        require(awsSecretAccessKey.isNotBlank()) { "AWS_SECRET_ACCESS_KEY must be set." }

        val credentials = BasicAWSCredentials(awsAccessKey, awsSecretAccessKey)

        return AmazonS3ClientBuilder
            .standard()
            .withCredentials(AWSStaticCredentialsProvider(credentials))
            .withRegion(awsRegion)
            .build()!!
    }

    // for running app on aws and using remote aws bucket
    private fun aws() = AmazonS3ClientBuilder.defaultClient()
}
