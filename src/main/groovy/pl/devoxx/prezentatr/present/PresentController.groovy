package pl.devoxx.prezentatr.present

import com.ofg.infrastructure.web.resttemplate.fluent.ServiceRestClient
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.sleuth.Trace
import org.springframework.cloud.sleuth.TraceScope
import org.springframework.cloud.sleuth.sampler.AlwaysSampler
import org.springframework.http.HttpEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.devoxx.prezentatr.config.Collaborators
import pl.devoxx.prezentatr.feed.FeedRepository
import pl.devoxx.prezentatr.feed.ProcessState

import static org.springframework.web.bind.annotation.RequestMethod.GET
import static org.springframework.web.bind.annotation.RequestMethod.POST
import static pl.devoxx.prezentatr.config.Versions.AGREGATR_CONTENT_TYPE_V1

@Slf4j
@RestController
@RequestMapping('/present')
@TypeChecked
@Api(value = "present", description = "API for GUI")
class PresentController {

    private ServiceRestClient restClient

    private FeedRepository feedRepository

    private Trace trace

    @Autowired
    public PresentController(ServiceRestClient restClient, FeedRepository feedRepository, Trace trace) {
        this.restClient = restClient
        this.feedRepository = feedRepository
        this.trace = trace
    }

    @RequestMapping(
            value = "/order",
            method = POST)
    @ApiOperation(value = "sends an order to agregatr")
    public String order(HttpEntity<String> body) {
        log.info("Making new order with $body.body")
        TraceScope scope = this.trace.startSpan("calling_aggregatr",
                new AlwaysSampler(), null);
        String result = restClient.forService(Collaborators.AGGREGATR_DEPENDENCY_NAME).post().onUrl("/ingredients")
                .body(body.body)
                    .withHeaders().contentType(AGREGATR_CONTENT_TYPE_V1)
                .andExecuteFor()
                .anObject()
                .ofType(String)
        scope.close()
        return result
    }

    @RequestMapping(value = "/dojrzewatr", method = GET)
    public String dojrzewatr() {
        return feedRepository.countFor(ProcessState.DOJRZEWATR)
    }

    @RequestMapping(value = "/butelkatr", method = GET)
    public String butelkatr() {
        return feedRepository.countFor(ProcessState.BUTELKATR)
    }

    @RequestMapping(value = "/bottles", method = GET)
    public String bottles() {
        return feedRepository.getBottles()
    }
}
