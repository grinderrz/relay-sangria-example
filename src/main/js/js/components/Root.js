import React, { Component, PropTypes } from 'react';
import { SubscriptionProvider } from 'relay-subscriptions';

// most of this is implementation details. You can solve this however you want
export default class Root extends Component {
  static propTypes = {
    environment: PropTypes.object.isRequired,
    children: PropTypes.element,
  };
  _subscriptions = [];
  subscribe = (subscriptionRequest, topic) => {
    this._subscriptions.push(subscriptionRequest);

    // register the subscription on the server
    var source = new EventSource('/graphql?query='+subscriptionRequest.getQueryString()+'&variables='+JSON.stringify(subscriptionRequest.getVariables()));
    // receives subscription payloads
    source.addEventListener('message', function(e) {
      //console.log('%s received data: %s', subscriptionRequest.getDebugName(), e.data);
      subscriptionRequest.onNext(JSON.parse(e.data).data);
    })
    source.addEventListener('close', function(e) {
      //console.log('%s is completed', subscriptionRequest.getDebugName());
      subscriptionRequest.onCompleted();
      this._subscriptions.splice(this._subscriptions.indexOf(subscriptionRequest), 1);
    })
  }
  render() {
    return (
      <SubscriptionProvider
        environment={this.props.environment}
        subscribe={this.subscribe}
      >
        {this.props.children}
      </SubscriptionProvider>
    );
  }
}
