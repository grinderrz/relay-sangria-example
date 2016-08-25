import Relay from 'react-relay'

export default class AddMessageMutation extends Relay.Mutation {
  static fragments = {
    chat: () => Relay.QL`
      fragment on Chat {
        id
      }
    `,
  };
  getMutation() {
    return Relay.QL`mutation{addMessage}`;
  }
  getFatQuery() {
    return Relay.QL`
      fragment on AddMessagePayload @relay(pattern: true) {
        messageEdge
      }
    `;
  }
  getConfigs() {
    return [{
      type: 'RANGE_ADD',
      //parentName: 'chat',
      parentID: this.props.chat.id,
      connectionName: 'messages',
      edgeName: 'messageEdge',
      rangeBehaviors: () => 'append',
    }];
  }
  getVariables() {
    return {
      message: this.props.message,
      chatId: this.props.chatId
    }
  }
  getOptimisticResponse() {
    return {
      messageEdge: {
        node: {
          content: this.props.message
        }
      }
    };
  }
}
