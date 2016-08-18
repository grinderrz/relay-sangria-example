import React from 'react';
import Relay from 'react-relay';

class MessageList extends React.Component {
  _onclickHistory = () => {
    console.log("get history");
  }
  renderMessages() {
    return this.props.viewer.messages.edges.map(edge =>
      <div key={edge.node.id}>{edge.node.content} (ID: {edge.node.rawId})</div>
    );
  }
  render() {
    return (
      <div>
        <h1>Messages</h1>
        <button name="history" onclick={this._onclickHistory}>history</button>
        <div>
          {this.renderMessages()}
        </div>
      </div>
    );
  }
}

export default Relay.createContainer(MessageList, {
  initialVariables: {
    last: 5
  },
  fragments: {
    viewer: () => Relay.QL`
      fragment on Chat {
        messages(last: $last) {
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
