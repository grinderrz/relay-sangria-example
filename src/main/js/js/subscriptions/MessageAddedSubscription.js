import Relay from 'react-relay';
import * as RelaySubscriptions from 'relay-subscriptions';

export default class MessageAddedSubscription extends RelaySubscriptions.Subscription {
  static fragments = {
    chat: () => Relay.QL`
    fragment on Chat {
      id
    }`,
  };
  getSubscription() {
    return Relay.QL`subscription {
      messageAdded(input: {clientMutationId: ""}) {
        clientMutationId
        messageEdge {
          __typename
          node {
            __typename
            id
            rawId
            content
          }
        }
      }
    }`;
  }
  getVariables() {
    return {};
  }
  getConfigs() {
    return [{
      type: 'RANGE_ADD',
      parentName: 'chat',
      parentID: this.props.chat.id,
      connectionName: 'messages',
      edgeName: 'messageEdge',
      rangeBehaviors: () => 'append',
    }];
  }
}
