import React from 'react';
import Relay from 'react-relay';

class App extends React.Component {
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

export default Relay.createContainer(App, {
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
