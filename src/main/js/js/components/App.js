import React from 'react';
import Relay from 'react-relay';
import * as RelaySubscriptions from 'relay-subscriptions';
import MessageAddedSubscription from '../subscriptions/MessageAddedSubscription';

class App extends React.Component {
  componentDidMount() {
    const subscribe = this.props.subscriptions.subscribe;
    this._addSubscription = subscribe(
      new MessageAddedSubscription({ chat: this.props.chat })
    );
  }
  componentWillUnmount() {
    if (this._addSubscription) this._addSubscription.dispose();
  }
  render() {
    return (
      <div>
        <h1>Messages</h1>
        <ul>
          {this.props.chat.messages.edges.map(edge =>
            <li key={edge.node.id}>{edge.node.content} (ID: {edge.node.rawId})</li>
          )}
        </ul>
      </div>
    );
  }
}

export default Relay.createContainer(RelaySubscriptions.createSubscriptionContainer(App), {
  fragments: {
    chat: () => Relay.QL`
      fragment on Chat {
        messages(last: 5) {
          edges {
            node {
              id,
              rawId,
              content,
            },
          },
        },
      }
    `,
  },
});
